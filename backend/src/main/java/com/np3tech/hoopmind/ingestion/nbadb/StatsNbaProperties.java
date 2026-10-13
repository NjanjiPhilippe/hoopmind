package com.np3tech.hoopmind.ingestion.nbadb;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Accès à stats.nba.com, pour compléter la base de Wyatt Walsh avec les saisons qu'elle n'a pas encore.
 * Le site est public : ni compte, ni clé.
 *
 * @param apiUrl  adresse de l'API (remplacée dans les tests)
 * @param delayMs pause entre deux requêtes, pour ne pas être bloqué par le site
 */
@ConfigurationProperties("hoopmind.import.stats-nba")
public record StatsNbaProperties(String apiUrl, Long delayMs) {

    public StatsNbaProperties {
        if (apiUrl == null || apiUrl.isBlank()) {
            apiUrl = "https://stats.nba.com/stats";
        }
        if (delayMs == null || delayMs < 0) {
            delayMs = 1_000L;
        }
    }
}
