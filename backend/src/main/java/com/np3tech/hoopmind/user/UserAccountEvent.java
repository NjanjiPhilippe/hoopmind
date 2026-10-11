package com.np3tech.hoopmind.user;

import com.np3tech.hoopmind.user.domain.RoleName;

import java.util.List;
import java.util.Set;

/**
 * Ce qui arrive à un compte utilisateur, publié avec {@code ApplicationEventPublisher}.
 * <p>
 * Le module ne sait pas qui écoute (aujourd'hui : le journal d'audit). Chaque événement désigne le compte
 * concerné ; l'auteur de l'action est l'utilisateur connecté au moment de la publication, ou à défaut le compte
 * lui-même. Un événement {@linkplain #failed() d'échec} est publié juste avant l'exception qui annule l'action.
 */
public sealed interface UserAccountEvent {

    long userId();

    String username();

    default boolean failed() {
        return false;
    }

    record Registered(long userId, String username) implements UserAccountEvent {
    }

    /** {@code fields} : noms des champs modifiés, jamais leurs valeurs. */
    record ProfileUpdated(long userId, String username, List<String> fields) implements UserAccountEvent {
        public ProfileUpdated {
            fields = List.copyOf(fields);
        }
    }

    record PasswordChanged(long userId, String username) implements UserAccountEvent {
    }

    record PasswordChangeRejected(long userId, String username) implements UserAccountEvent {
        @Override
        public boolean failed() {
            return true;
        }
    }

    record EnabledChanged(long userId, String username, boolean enabled) implements UserAccountEvent {
    }

    record RolesChanged(long userId, String username, Set<RoleName> before, Set<RoleName> after)
            implements UserAccountEvent {
        public RolesChanged {
            before = Set.copyOf(before);
            after = Set.copyOf(after);
        }
    }

    record Deleted(long userId, String username) implements UserAccountEvent {
    }

    /** Administrateur initial créé au démarrage : l'auteur est le système. */
    record AdminBootstrapped(long userId, String username) implements UserAccountEvent {
    }

    /** Refus de désactiver, rétrograder ou supprimer le dernier administrateur actif. */
    record LastAdminProtected(long userId, String username, Attempt attempt) implements UserAccountEvent {

        public enum Attempt { DISABLE, CHANGE_ROLES, DELETE }

        @Override
        public boolean failed() {
            return true;
        }
    }
}
