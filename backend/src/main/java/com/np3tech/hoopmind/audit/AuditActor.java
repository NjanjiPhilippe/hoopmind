package com.np3tech.hoopmind.audit;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;

/**
 * Auteur d'une action : un utilisateur (identifiant et nom au moment de l'action) ou le système.
 */
public record AuditActor(Long id, String login) {

    public static final AuditActor SYSTEM = new AuditActor(null, "système");

    public static AuditActor user(Long id, String login) {
        return new AuditActor(id, login);
    }

    /**
     * L'utilisateur authentifié de la requête en cours, s'il y en a un.
     */
    public static Optional<AuditActor> current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        if (authentication instanceof JwtAuthenticationToken jwt) {
            return Optional.of(new AuditActor(Long.valueOf(jwt.getName()), jwt.getToken().getClaimAsString("username")));
        }
        return Optional.of(new AuditActor(null, authentication.getName()));
    }
}
