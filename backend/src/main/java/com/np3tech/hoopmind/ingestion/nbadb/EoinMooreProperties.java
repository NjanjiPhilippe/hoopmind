package com.np3tech.hoopmind.ingestion.nbadb;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Fichiers du jeu Kaggle d'Eoin Moore ({@code eoinamoore/historical-nba-data-and-player-box-scores}), pour les saisons
 * que la base de Wyatt Walsh n'a pas encore.
 *
 * @param directory dossier du serveur où déposer {@code Games.csv} et {@code TeamStatistics.csv}
 */
@ConfigurationProperties("hoopmind.import.eoin-moore")
public record EoinMooreProperties(String directory) {

    public EoinMooreProperties {
        if (directory == null || directory.isBlank()) {
            directory = "data/eoinamoore";
        }
    }
}
