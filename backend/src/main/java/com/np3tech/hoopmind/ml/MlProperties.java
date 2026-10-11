package com.np3tech.hoopmind.ml;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Réglages du modèle de prédiction ({@code hoopmind.ml.*}).
 *
 * @param trainingSeasons nombre de saisons (les plus récentes) utilisées par défaut pour l'entraînement
 * @param modelDir        dossier où le modèle entraîné est enregistré (relu au redémarrage)
 */
@ConfigurationProperties("hoopmind.ml")
public record MlProperties(
        @DefaultValue("5") int trainingSeasons,
        @DefaultValue("spark-models") String modelDir
) {
}
