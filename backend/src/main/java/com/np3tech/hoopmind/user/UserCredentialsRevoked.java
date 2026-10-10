package com.np3tech.hoopmind.user;

/**
 * Publié quand les sessions existantes d'un utilisateur ne doivent plus être acceptées :
 * changement de mot de passe, désactivation, changement de rôles ou suppression du compte.
 */
public record UserCredentialsRevoked(Long userId, Reason reason) {

    public enum Reason {
        PASSWORD_CHANGED,
        ACCOUNT_DISABLED,
        ROLES_CHANGED,
        ACCOUNT_DELETED
    }
}
