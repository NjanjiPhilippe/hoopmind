package com.np3tech.hoopmind.audit;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Événement à tracer, publié avec {@code ApplicationEventPublisher}.
 * <p>
 * Une réussite n'est enregistrée que si la transaction de l'appelant est validée ; un échec l'est toujours,
 * y compris quand l'exception qui le signale annule la transaction.
 *
 * @param actor   auteur de l'action, {@code null} s'il est inconnu (connexion avec un identifiant inexistant)
 * @param target  objet concerné, {@code null} s'il n'y en a pas
 * @param details informations complémentaires courtes ; jamais de mot de passe, de jeton ni de valeur saisie
 */
public record AuditEvent(AuditAction action, AuditOutcome outcome, AuditActor actor, AuditTarget target,
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

    public static AuditEvent success(AuditAction action, AuditActor actor, AuditTarget target) {
        return new AuditEvent(action, AuditOutcome.SUCCESS, actor, target, Map.of());
    }

    public static AuditEvent failure(AuditAction action, AuditActor actor, AuditTarget target, String reason) {
        return new AuditEvent(action, AuditOutcome.FAILURE, actor, target, Map.of("reason", reason));
    }

    public AuditEvent withDetail(String key, String value) {
        Map<String, String> copy = new LinkedHashMap<>(details);
        copy.put(key, value);
        return new AuditEvent(action, outcome, actor, target, copy);
    }
}
