package com.np3tech.hoopmind.auth;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;

/**
 * L'utilisateur authentifié de la requête en cours. Seul le module auth connaît le format du jeton
 * (sujet = identifiant, claim {@code username}) : les autres modules passent par ici.
 */
public record CurrentUser(Long id, String username) {

    public static Optional<CurrentUser> get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        if (authentication instanceof JwtAuthenticationToken jwt) {
            return Optional.of(new CurrentUser(Long.valueOf(jwt.getName()), jwt.getToken().getClaimAsString("username")));
        }
        return Optional.of(new CurrentUser(null, authentication.getName()));
    }
}
