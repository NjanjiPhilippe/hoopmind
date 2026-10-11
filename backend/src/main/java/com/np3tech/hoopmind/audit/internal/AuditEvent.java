package com.np3tech.hoopmind.audit.internal;

import com.np3tech.hoopmind.audit.AuditAction;
import com.np3tech.hoopmind.audit.AuditOutcome;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Entrée à tracer, construite par {@link AuditTranslator} à partir d'un événement d'un autre module.
 *
 * @param actor   auteur de l'action, {@code null} s'il est inconnu (connexion avec un identifiant inexistant)
 * @param target  objet concerné, {@code null} s'il n'y en a pas
 * @param details informations complémentaires courtes ; jamais de mot de passe, de jeton ni de valeur saisie
 */
record AuditEvent(AuditAction action, AuditOutcome outcome, AuditActor actor, AuditTarget target,
                         Map<String, String> details) {

    public AuditEvent {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(outcome, "outcome");
        Map<String, String> copy = new LinkedHashMap<>();
        if (details != null) {
            details.forEach((key, value) -> {
                if (value != null) copy.put(key, value);
            });
        }
        details = Collections.unmodifiableMap(copy);
    }

    static AuditEvent success(AuditAction action, AuditActor actor, AuditTarget target) {
        return new AuditEvent(action, AuditOutcome.SUCCESS, actor, target, Map.of());
    }

    static AuditEvent failure(AuditAction action, AuditActor actor, AuditTarget target, String reason) {
        return new AuditEvent(action, AuditOutcome.FAILURE, actor, target, Map.of("reason", reason));
    }

    AuditEvent withDetail(String key, String value) {
        Map<String, String> copy = new LinkedHashMap<>(details);
        copy.put(key, value);
        return new AuditEvent(action, outcome, actor, target, copy);
    }
}
