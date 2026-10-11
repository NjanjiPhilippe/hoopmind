package com.np3tech.hoopmind.audit.web;

import com.np3tech.hoopmind.audit.AuditAction;
import com.np3tech.hoopmind.audit.AuditOutcome;
import com.np3tech.hoopmind.audit.internal.AuditEntry;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(name = "AuditEvent", requiredProperties = {"id", "occurredAt", "action", "outcome", "details"})
record AuditEventView(
        Long id,
        Instant occurredAt,
        AuditAction action,
        AuditOutcome outcome,
        @Schema(description = "Absent pour une action du système ou une connexion avec un identifiant inconnu") Long actorId,
        String actorLogin,
        String targetType,
        String targetId,
        String targetLabel,
        Map<String, String> details) {

    static AuditEventView of(AuditEntry entry) {
        return new AuditEventView(entry.getId(), entry.getOccurredAt(), entry.getAction(), entry.getOutcome(),
                entry.getActorId(), entry.getActorLogin(), entry.getTargetType(), entry.getTargetId(),
                entry.getTargetLabel(), entry.getDetails());
    }
}
