package com.np3tech.hoopmind.auth;

/**
 * Événements de sécurité des sessions, publiés avec {@code ApplicationEventPublisher}.
 * <p>
 * Ils désignent le compte concerné quand il est connu. L'identifiant saisi lors d'une connexion échouée
 * n'est jamais transporté : il peut contenir un mot de passe tapé dans le mauvais champ.
 */
public sealed interface AuthenticationEvent {

    /** Compte concerné, {@code null} si l'identifiant saisi ne correspond à aucun compte. */
    Long userId();

    String username();

    default boolean failed() {
        return false;
    }

    record LoginSucceeded(Long userId, String username) implements AuthenticationEvent {
    }

    record LoginFailed(Long userId, String username, Reason reason) implements AuthenticationEvent {

        public enum Reason { UNKNOWN_ACCOUNT, BAD_PASSWORD, ACCOUNT_DISABLED }

        public static LoginFailed unknownAccount() {
            return new LoginFailed(null, null, Reason.UNKNOWN_ACCOUNT);
        }

        @Override
        public boolean failed() {
            return true;
        }
    }

    record LoggedOut(Long userId, String username) implements AuthenticationEvent {
    }

    /**
     * Connexion refusée sans vérifier le mot de passe : trop d'échecs récents depuis cette adresse.
     * Publié une seule fois par blocage, pour qu'une attaque ne remplisse pas le journal.
     */
    record LoginThrottled(Long userId, String username, long retryAfterSeconds) implements AuthenticationEvent {
        @Override
        public boolean failed() {
            return true;
        }
    }

    /** Un jeton de rafraîchissement déjà utilisé a été présenté : toute sa famille vient d'être révoquée. */
    record RefreshTokenReused(Long userId, String username, int revokedTokens) implements AuthenticationEvent {
        @Override
        public boolean failed() {
            return true;
        }
    }
}
