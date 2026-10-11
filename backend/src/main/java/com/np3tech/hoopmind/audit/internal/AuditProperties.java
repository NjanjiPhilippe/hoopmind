package com.np3tech.hoopmind.audit.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * @param retention durée de conservation des événements ; les plus anciens sont supprimés chaque nuit
 */
@ConfigurationProperties("hoopmind.audit")
record AuditProperties(@DefaultValue("365d") Duration retention) {
}
