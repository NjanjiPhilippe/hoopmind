package com.np3tech.hoopmind.auth.internal;

import com.np3tech.hoopmind.user.UserCredentialsRevoked;
import org.springframework.context.event.EventListener;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Refuse tout de suite les jetons d'accès d'un compte désactivé, supprimé ou dont les rôles ont changé. Sans cela,
 * le jeton (sans état, 15 minutes) restait valable jusqu'à son expiration, rôle d'administrateur compris.
 * <p>
 * Chaque compte révoqué a un numéro de version, inscrit dans ses jetons (claim {@value #CLAIM}) : un jeton d'une
 * version antérieure est refusé, un jeton émis après la révocation (nouvelle connexion) est accepté. Une date
 * d'émission ne suffirait pas : elle est à la seconde, et ne départage pas deux jetons émis dans la même seconde.
 * <p>
 * Un changement de mot de passe n'est pas concerné : il révoque les autres sessions (jetons de rafraîchissement),
 * l'utilisateur qui vient de le changer garde la sienne. L'état est en mémoire : un redémarrage l'efface, mais un jeton
 * émis avant ne vit de toute façon que 15 minutes. Il ne compte qu'une entrée par compte révoqué.
 */
@Component
public class RevokedAccessTokens implements OAuth2TokenValidator<Jwt> {

    static final String CLAIM = "rev";

    private static final OAuth2Error REVOKED = new OAuth2Error("invalid_token", "Jeton révoqué", null);

    private final ConcurrentMap<String, Long> versions = new ConcurrentHashMap<>();

    @EventListener
    void on(UserCredentialsRevoked event) {
        if (event.reason() != UserCredentialsRevoked.Reason.PASSWORD_CHANGED) {
            versions.merge(String.valueOf(event.userId()), 1L, Long::sum);
        }
    }

    /** Version à inscrire dans un nouveau jeton de ce compte. */
    long version(Long userId) {
        return versions.getOrDefault(String.valueOf(userId), 0L);
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        Long current = versions.get(token.getSubject());
        if (current == null) {
            return OAuth2TokenValidatorResult.success();
        }
        Number version = token.getClaim(CLAIM);
        return version == null || version.longValue() < current
                ? OAuth2TokenValidatorResult.failure(REVOKED)
                : OAuth2TokenValidatorResult.success();
    }
}
