package com.np3tech.hoopmind.audit.internal;

import com.np3tech.hoopmind.audit.AuditAction;
import com.np3tech.hoopmind.audit.AuditEvent;
import com.np3tech.hoopmind.audit.AuditOutcome;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ligne du journal d'audit. Les noms de l'auteur et de la cible sont copiés pour rester lisibles
 * après la suppression ou le renommage d'un compte ; il n'y a donc pas de clé étrangère.
 */
@Entity
@Table(name = "audit_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50, updatable = false)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private AuditOutcome outcome;

    @Column(name = "actor_id", updatable = false)
    private Long actorId;

    @Column(name = "actor_login", length = 100, updatable = false)
    private String actorLogin;

    @Column(name = "target_type", length = 30, updatable = false)
    private String targetType;

    @Column(name = "target_id", length = 64, updatable = false)
    private String targetId;

    @Column(name = "target_label", length = 100, updatable = false)
    private String targetLabel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false)
    private Map<String, String> details = new LinkedHashMap<>();

    static AuditEntry of(AuditEvent event, Instant occurredAt) {
        AuditEntry entry = new AuditEntry();
        entry.occurredAt = occurredAt;
        entry.action = event.action();
        entry.outcome = event.outcome();
        if (event.actor() != null) {
            entry.actorId = event.actor().id();
            entry.actorLogin = truncate(event.actor().login(), 100);
        }
        if (event.target() != null) {
            entry.targetType = event.target().type();
            entry.targetId = truncate(event.target().id(), 64);
            entry.targetLabel = truncate(event.target().label(), 100);
        }
        entry.details = new LinkedHashMap<>(event.details());
        return entry;
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
