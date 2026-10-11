package com.np3tech.hoopmind.auth.internal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Freine les attaques par essais successifs sur {@code POST /auth/login}.
 * <p>
 * Deux compteurs d'échecs : identifiant + adresse IP (un attaquant qui vise un compte) et adresse IP seule
 * (un attaquant qui essaie un mot de passe courant sur beaucoup de comptes). Au-delà du seuil, l'adresse est
 * bloquée pour une durée qui double à chaque nouvel échec. Le blocage ne vise jamais le compte seul : un
 * attaquant ne peut pas empêcher le vrai propriétaire de se connecter depuis une autre adresse.
 * <p>
 * L'état est en mémoire : il est perdu au redémarrage et n'est pas partagé entre plusieurs instances de l'API.
 */
@Slf4j
@Component
@EnableConfigurationProperties(LoginThrottleProperties.class)
class LoginThrottle {

    /** Au-delà, on purge d'abord les entrées périmées puis, si besoin, les compteurs par identifiant. */
    static final int MAX_TRACKED_KEYS = 100_000;

    /** Refus en cours. {@code firstRejection} : premier refus de ce blocage (seul celui-ci est tracé). */
    record Rejection(Duration retryAfter, boolean firstRejection) {
    }

    private record Attempts(int failures, Instant lastFailure, Instant blockedUntil, boolean rejectionReported) {
    }

    private final ConcurrentMap<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final LoginThrottleProperties properties;
    private final Clock clock;

    LoginThrottle(LoginThrottleProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    Optional<Rejection> check(String login, String address) {
        Instant now = clock.instant();
        Rejection byLogin = rejection(loginKey(login, address), now);
        Rejection byAddress = rejection(addressKey(address), now);
        if (byLogin == null) return Optional.ofNullable(byAddress);
        if (byAddress == null) return Optional.of(byLogin);
        return Optional.of(new Rejection(max(byLogin.retryAfter(), byAddress.retryAfter()),
                byLogin.firstRejection() || byAddress.firstRejection()));
    }

    void failed(String login, String address) {
        Instant now = clock.instant();
        if (attempts.size() >= MAX_TRACKED_KEYS) {
            evict(now);
        }
        record(loginKey(login, address), properties.maxFailuresPerLogin(), now);
        record(addressKey(address), properties.maxFailuresPerAddress(), now);
    }

    /** Connexion réussie : on oublie les échecs de cet identifiant depuis cette adresse (pas ceux de l'adresse). */
    void succeeded(String login, String address) {
        attempts.remove(loginKey(login, address));
    }

    @Scheduled(fixedDelayString = "${hoopmind.security.login-throttle.cleanup-interval:5m}")
    void purgeExpired() {
        Instant now = clock.instant();
        attempts.entrySet().removeIf(entry -> expired(entry.getValue(), now));
    }

    private Rejection rejection(String key, Instant now) {
        Attempts[] before = new Attempts[1];
        Attempts current = attempts.computeIfPresent(key, (k, value) -> {
            before[0] = value;
            if (value.blockedUntil() == null || !now.isBefore(value.blockedUntil())) {
                return value;
            }
            return new Attempts(value.failures(), value.lastFailure(), value.blockedUntil(), true);
        });
        if (current == null || current.blockedUntil() == null || !now.isBefore(current.blockedUntil())) {
            return null;
        }
        return new Rejection(Duration.between(now, current.blockedUntil()), !before[0].rejectionReported());
    }

    private void record(String key, int threshold, Instant now) {
        attempts.compute(key, (k, value) -> {
            int failures = value == null || expired(value, now) ? 1 : value.failures() + 1;
            Instant blockedUntil = failures < threshold ? null : now.plus(block(failures - threshold));
            return new Attempts(failures, now, blockedUntil, false);
        });
    }

    /** 30 s, 1 min, 2 min, 4 min… plafonné. */
    private Duration block(int failuresBeyondThreshold) {
        int doublings = Math.min(failuresBeyondThreshold, 20);
        Duration block = properties.initialBlock().multipliedBy(1L << doublings);
        return max(properties.initialBlock(), block.compareTo(properties.maxBlock()) > 0 ? properties.maxBlock() : block);
    }

    /** Compté à partir de la fin du blocage : attendre la fin d'un blocage ne remet pas le compteur à zéro. */
    private boolean expired(Attempts value, Instant now) {
        Instant last = value.blockedUntil() != null && value.blockedUntil().isAfter(value.lastFailure())
                ? value.blockedUntil() : value.lastFailure();
        return last.plus(properties.resetAfter()).isBefore(now);
    }

    private void evict(Instant now) {
        purgeExpired();
        if (attempts.size() >= MAX_TRACKED_KEYS) {
            log.warn("Trop de compteurs de connexion ({}) : les compteurs par identifiant sont réinitialisés", attempts.size());
            attempts.keySet().removeIf(key -> key.startsWith("login:"));
        }
    }

    private static String loginKey(String login, String address) {
        return "login:" + address + ":" + (login == null ? "" : login.trim().toLowerCase(Locale.ROOT));
    }

    private static String addressKey(String address) {
        return "address:" + address;
    }

    private static Duration max(Duration a, Duration b) {
        return a.compareTo(b) >= 0 ? a : b;
    }
}
