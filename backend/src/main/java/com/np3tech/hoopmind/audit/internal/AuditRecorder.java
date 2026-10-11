package com.np3tech.hoopmind.audit.internal;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;

/**
 * Enregistre les événements d'audit dans leur propre transaction, après celle de l'action :
 * une erreur d'écriture du journal ne fait jamais échouer l'action elle-même.
 */
@Slf4j
@Component
@EnableConfigurationProperties(AuditProperties.class)
class AuditRecorder {

    /** Journal applicatif dédié, que l'on peut router vers un fichier ou un outil de collecte. */
    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("hoopmind.audit");

    private final AuditEntryRepository entries;
    private final TransactionTemplate newTransaction;
    private final AuditProperties properties;
    private final Clock clock;

    AuditRecorder(AuditEntryRepository entries, PlatformTransactionManager transactionManager,
                  AuditProperties properties, Clock clock) {
        this.entries = entries;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.properties = properties;
        this.clock = clock;
    }

    void record(AuditEvent event) {
        Instant now = clock.instant();
        AUDIT_LOG.info("action={} outcome={} actor={} target={} details={}", event.action(), event.outcome(),
                event.actor() == null ? "-" : event.actor().login() + "#" + event.actor().id(),
                event.target() == null ? "-" : event.target().type() + ":" + event.target().id(),
                event.details());
        try {
            newTransaction.executeWithoutResult(status -> entries.save(AuditEntry.of(event, now)));
        } catch (RuntimeException e) {
            log.error("Impossible d'enregistrer l'événement d'audit {}", event.action(), e);
        }
    }

    @Scheduled(cron = "${hoopmind.audit.cleanup-cron:0 30 3 * * *}")
    @Transactional
    public void purgeExpired() {
        int deleted = entries.deleteOlderThan(clock.instant().minus(properties.retention()));
        if (deleted > 0) {
            log.info("{} événement(s) d'audit de plus de {} supprimé(s)", deleted, properties.retention());
        }
    }
}
