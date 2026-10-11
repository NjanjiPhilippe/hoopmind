package com.np3tech.hoopmind.auth.internal;

import com.np3tech.hoopmind.auth.AuthenticationEvent;
import com.np3tech.hoopmind.auth.AuthenticationEvent.LoginFailed;
import com.np3tech.hoopmind.auth.config.JwtProperties;
import com.np3tech.hoopmind.user.Registration;
import com.np3tech.hoopmind.user.UserAccount;
import com.np3tech.hoopmind.user.UserAccountService;
import com.np3tech.hoopmind.user.UserCredentialsRevoked;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Connexion, rotation des jetons de rafraîchissement et déconnexion.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserAccountService accounts;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenIssuer accessTokens;
    private final RefreshTokenRepository refreshTokens;
    private final JwtProperties properties;
    private final Clock clock;
    private final ApplicationEventPublisher events;
    private final LoginThrottle throttle;

    /** Hash factice comparé quand le compte n'existe pas, pour ne pas révéler son existence par le temps de réponse. */
    private String dummyHash;

    @Transactional
    public Session register(Registration registration, ClientInfo client) {
        UserAccount account = accounts.register(registration);
        return openSession(account, UUID.randomUUID().toString(), client);
    }

    @Transactional
    public Session login(String login, String password, ClientInfo client) {
        // Vérifié avant le mot de passe : un attaquant bloqué ne peut plus rien tester, même le bon mot de passe.
        throttle.check(login, client.ipAddress()).ifPresent(rejection -> {
            if (rejection.firstRejection()) {
                UserAccount target = accounts.findAccountByLogin(login).orElse(null);
                events.publishEvent(new AuthenticationEvent.LoginThrottled(target == null ? null : target.id(),
                        target == null ? null : target.username(), rejection.retryAfter().toSeconds()));
            }
            throw new LoginThrottledException(rejection.retryAfter());
        });
        UserAccount account = accounts.findAccountByLogin(login).orElse(null);
        if (account == null) {
            passwordEncoder.matches(password, dummyHash());
            throttle.failed(login, client.ipAddress());
            events.publishEvent(LoginFailed.unknownAccount());
            throw AuthenticationFailedException.badCredentials();
        }
        if (!passwordEncoder.matches(password, account.passwordHash())) {
            throttle.failed(login, client.ipAddress());
            loginFailed(account, LoginFailed.Reason.BAD_PASSWORD);
        }
        if (!account.active()) {
            loginFailed(account, LoginFailed.Reason.ACCOUNT_DISABLED);
        }
        throttle.succeeded(login, client.ipAddress());
        accounts.recordSuccessfulLogin(account.id());
        events.publishEvent(new AuthenticationEvent.LoginSucceeded(account.id(), account.username()));
        return openSession(account, UUID.randomUUID().toString(), client);
    }

    /**
     * Échange un jeton de rafraîchissement contre un nouveau couple de jetons. L'ancien jeton est révoqué ;
     * s'il est présenté de nouveau (vol probable), toute la famille est révoquée.
     */
    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public Session refresh(String rawRefreshToken, ClientInfo client) {
        Instant now = clock.instant();
        RefreshToken current = refreshTokens.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(AuthenticationFailedException::invalidRefreshToken);

        if (current.isRevoked()) {
            int revoked = refreshTokens.revokeFamily(current.getFamilyId(), now);
            log.warn("Réutilisation d'un jeton de rafraîchissement révoqué (utilisateur {}), {} jeton(s) révoqué(s)",
                    current.getUserId(), revoked);
            accounts.findAccountById(current.getUserId()).ifPresent(account -> events.publishEvent(
                    new AuthenticationEvent.RefreshTokenReused(account.id(), account.username(), revoked)));
            throw AuthenticationFailedException.invalidRefreshToken();
        }
        if (current.isExpired(now)) {
            throw AuthenticationFailedException.invalidRefreshToken();
        }
        UserAccount account = accounts.findAccountById(current.getUserId())
                .filter(UserAccount::active)
                .orElseThrow(AuthenticationFailedException::invalidRefreshToken);

        current.revoke(now);
        return openSession(account, current.getFamilyId(), client);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokens.findByTokenHash(hash(rawRefreshToken)).ifPresent(token -> {
            refreshTokens.revokeFamily(token.getFamilyId(), clock.instant());
            if (!token.isRevoked()) {
                accounts.findAccountById(token.getUserId()).ifPresent(account -> events.publishEvent(
                        new AuthenticationEvent.LoggedOut(account.id(), account.username())));
            }
        });
    }

    /**
     * Synchrone et dans la transaction de l'appelant : un compte désactivé ou supprimé
     * perd immédiatement ses sessions.
     */
    @EventListener
    void on(UserCredentialsRevoked event) {
        if (event.reason() == UserCredentialsRevoked.Reason.ACCOUNT_DELETED) {
            refreshTokens.deleteAllForUser(event.userId());
        } else {
            refreshTokens.revokeAllForUser(event.userId(), clock.instant());
        }
    }

    @Scheduled(cron = "${hoopmind.security.refresh-token-cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void purgeExpiredRefreshTokens() {
        int deleted = refreshTokens.deleteExpiredBefore(clock.instant());
        if (deleted > 0) {
            log.info("{} jeton(s) de rafraîchissement expiré(s) supprimé(s)", deleted);
        }
    }

    private Session openSession(UserAccount account, String familyId, ClientInfo client) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawRefreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshTokens.save(new RefreshToken(account.id(), hash(rawRefreshToken), familyId,
                clock.instant().plus(properties.refreshTokenTtl()),
                truncate(client.ipAddress(), 100), truncate(client.userAgent(), 500)));
        return new Session(account.id(), accessTokens.issue(account), properties.accessTokenTtl().toSeconds(),
                rawRefreshToken, properties.refreshTokenTtl().toSeconds());
    }

    private void loginFailed(UserAccount account, LoginFailed.Reason reason) {
        events.publishEvent(new LoginFailed(account.id(), account.username(), reason));
        throw AuthenticationFailedException.badCredentials();
    }

    private String dummyHash() {
        if (dummyHash == null) {
            dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
        }
        return dummyHash;
    }

    static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    public record Session(Long userId, String accessToken, long accessTokenExpiresIn,
                          String refreshToken, long refreshTokenExpiresIn) {
    }

    public record ClientInfo(String ipAddress, String userAgent) {
    }
}
