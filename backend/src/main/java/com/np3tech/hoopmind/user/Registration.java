package com.np3tech.hoopmind.user;

/**
 * Données d'inscription, déjà validées par la couche web.
 */
public record Registration(String username, String email, String password, String firstName, String lastName) {
}
