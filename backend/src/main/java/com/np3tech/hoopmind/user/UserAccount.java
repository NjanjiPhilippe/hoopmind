package com.np3tech.hoopmind.user;

import com.np3tech.hoopmind.user.domain.RoleName;

import java.util.Set;

/**
 * Vue d'un compte destinée à l'authentification. Contient le hash du mot de passe :
 * ne jamais la sérialiser vers un client.
 */
public record UserAccount(
        Long id,
        String username,
        String email,
        String passwordHash,
        Set<RoleName> roles,
        boolean active
) {
}
