package com.np3tech.hoopmind.nba.importer;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Réglages de l'import de la base NBA de Wyatt Walsh (Kaggle {@code wyattowalsh/basketball}).
 *
 * @param path      chemin du fichier {@code nba.sqlite} téléchargé (voir {@code scripts/download-nba-database.sh})
 * @param seasons   nombre de saisons à importer, en partant de la plus récente
 * @param onStartup lancer l'import au démarrage de l'application
 */
@ConfigurationProperties("hoopmind.import.nba-database")
public record NbaDatabaseImportProperties(String path, int seasons, boolean onStartup) {

    public NbaDatabaseImportProperties {
        if (path == null || path.isBlank()) {
            path = "data/nba.sqlite";
        }
        if (seasons <= 0) {
            seasons = 10;
        }
    }
}
