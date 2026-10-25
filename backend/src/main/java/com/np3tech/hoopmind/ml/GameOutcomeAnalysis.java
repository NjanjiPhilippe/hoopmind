package com.np3tech.hoopmind.ml;

import java.util.List;

/**
 * Une prédiction et son détail : ce que chaque variable a pesé. C'est la matière première de l'explication en
 * langage naturel : le LLM ne voit que ces chiffres, il ne recalcule rien.
 *
 * @param modelVersion version du modèle entraîné (change à chaque entraînement)
 * @param intercept    ordonnée à l'origine du modèle : la marge quand toutes les variables sont dans la moyenne
 * @param factors      variables dans l'ordre du modèle
 * @param homeGamesPlayed matchs joués cette saison par l'équipe à domicile avant ce match : sous
 *                        {@code TeamForm.PRIOR_GAMES}, sa forme repose surtout sur la saison passée
 * @param awayGamesPlayed même chose pour l'équipe à l'extérieur
 */
public record GameOutcomeAnalysis(GameOutcomePrediction prediction, String modelVersion, double intercept,
                                  List<PredictionFactor> factors, long homeGamesPlayed, long awayGamesPlayed) {

    public GameOutcomeAnalysis {
        factors = List.copyOf(factors);
    }
}
