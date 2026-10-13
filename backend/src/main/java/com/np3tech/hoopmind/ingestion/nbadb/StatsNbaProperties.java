package com.np3tech.hoopmind.ingestion.nbadb;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Accès à stats.nba.com, pour compléter la base de Wyatt Walsh avec les saisons qu'elle n'a pas encore.
 * Le site est public : ni compte, ni clé.
 *
 * @param apiUrl  adresse de l'API (remplacée dans les tests)
 * @param delayMs        pause entre deux requêtes : le site cesse de répondre aux clients trop pressés
 * @param timeoutSeconds attente maximale d'une réponse ; le site ne refuse pas un client bloqué, il le laisse attendre
 */
@ConfigurationProperties("hoopmind.import.stats-nba")
public record StatsNbaProperties(String apiUrl, Long delayMs, Long timeoutSeconds) {

    public StatsNbaProperties {
        if (apiUrl == null || apiUrl.isBlank()) {
            apiUrl = "https://stats.nba.com/stats";
        }
        if (delayMs == null || delayMs < 0) {
            delayMs = 3_000L;
        }
        if (timeoutSeconds == null || timeoutSeconds <= 0) {
            timeoutSeconds = 30L;
        }
    }
}
