package com.np3tech.hoopmind.ml;

import java.util.List;

/**
 * Une prédiction et son détail : ce que chaque variable a pesé. C'est la matière première de l'explication en
 * langage naturel : le LLM ne voit que ces chiffres, il ne recalcule rien.
 *
 * @param modelVersion version du modèle entraîné (change à chaque entraînement)
 * @param intercept    ordonnée à l'origine du modèle : la marge quand toutes les variables sont dans la moyenne
 * @param factors      variables dans l'ordre du modèle
 */
public record GameOutcomeAnalysis(GameOutcomePrediction prediction, String modelVersion, double intercept,
                                  List<PredictionFactor> factors) {

    public GameOutcomeAnalysis {
        factors = List.copyOf(factors);
    }
}
