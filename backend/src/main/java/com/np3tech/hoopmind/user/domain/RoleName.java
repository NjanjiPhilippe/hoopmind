package com.np3tech.hoopmind.user.domain;

/**
 * Rôles applicatifs. Le nom correspond à la colonne {@code roles.name} et à l'autorité Spring Security.
 */
public enum RoleName {
    ROLE_USER,
    ROLE_ADMIN
}
