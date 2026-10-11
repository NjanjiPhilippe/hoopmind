package com.np3tech.hoopmind.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Active les tâches planifiées : purge des jetons de rafraîchissement expirés et du journal d'audit.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
