package com.np3tech.hoopmind.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * @param secret          clé HMAC encodée en Base64, au moins 256 bits ({@code JWT_SECRET})
 * @param issuer          émetteur inscrit et vérifié dans chaque jeton
 * @param accessTokenTtl  durée de vie d'un jeton d'accès
 * @param refreshTokenTtl durée de vie d'un jeton de rafraîchissement
 * @param refreshCookie   cookie HttpOnly qui transporte le jeton de rafraîchissement
 */
@ConfigurationProperties("hoopmind.security.jwt")
public record JwtProperties(
        String secret,
        @DefaultValue("hoopmind") String issuer,
        @DefaultValue("15m") Duration accessTokenTtl,
        @DefaultValue("7d") Duration refreshTokenTtl,
        @DefaultValue RefreshCookie refreshCookie
) {

    public record RefreshCookie(
            @DefaultValue("hoopmind_refresh") String name,
            @DefaultValue("/api/v1/auth") String path,
            @DefaultValue("true") boolean secure,
            @DefaultValue("Strict") String sameSite
    ) {
    }
}
