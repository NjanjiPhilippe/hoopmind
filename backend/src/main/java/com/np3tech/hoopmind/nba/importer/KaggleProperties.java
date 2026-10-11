package com.np3tech.hoopmind.nba.importer;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Accès à l'API Kaggle, pour que le serveur télécharge lui-même la base NBA de Wyatt Walsh.
 *
 * @param username nom d'utilisateur Kaggle ({@code KAGGLE_USERNAME})
 * @param key      clé d'API Kaggle ({@code KAGGLE_KEY}, créée sur https://www.kaggle.com/settings), jamais versionnée
 * @param dataset  jeu de données Kaggle ({@code propriétaire/nom})
 * @param file     fichier du jeu de données à télécharger
 * @param apiUrl   adresse de l'API Kaggle (remplacée dans les tests)
 */
@ConfigurationProperties("hoopmind.import.kaggle")
public record KaggleProperties(String username, String key, String dataset, String file, String apiUrl) {

    public KaggleProperties {
        if (dataset == null || dataset.isBlank()) {
            dataset = "wyattowalsh/basketball";
        }
        if (file == null || file.isBlank()) {
            file = "nba.sqlite";
        }
        if (apiUrl == null || apiUrl.isBlank()) {
            apiUrl = "https://www.kaggle.com/api/v1";
        }
    }

    public boolean configured() {
        return username != null && !username.isBlank() && key != null && !key.isBlank();
    }
}
