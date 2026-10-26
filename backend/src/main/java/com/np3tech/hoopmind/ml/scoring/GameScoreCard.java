package com.np3tech.hoopmind.ml.scoring;

import java.util.List;

/**
 * Les deux régressions linéaires du modèle de score (points de l'équipe à domicile, points de l'équipe à l'extérieur),
 * appliquées sans Spark. Chacune est une {@link ScoringCard} : même standardisation, mêmes variables.
 *
 * @param marginError erreur moyenne sur l'écart, mesurée sur la saison de test (en points) : l'ordre de grandeur de
 *                    l'incertitude à montrer à côté de chaque score prédit
 */
public record GameScoreCard(String model, String version, ScoringCard homePoints, ScoringCard awayPoints,
                            double marginError) {

    public boolean matches(List<String> expectedFeatures) {
        return homePoints.matches(expectedFeatures) && awayPoints.matches(expectedFeatures);
    }
}
