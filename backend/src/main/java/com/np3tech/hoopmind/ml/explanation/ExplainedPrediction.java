package com.np3tech.hoopmind.ml.explanation;

import com.np3tech.hoopmind.ml.GameOutcomePrediction;
import com.np3tech.hoopmind.ml.PredictionFactor;

import java.util.List;

/**
 * Une prédiction, ce qui l'explique en chiffres et en mots.
 *
 * @param factors  variables du modèle, de la plus à la moins influente
 * @param cached   explication déjà générée pour ce match, cette date et cette version du modèle
 * @param fallback le LLM configuré n'a pas répondu : l'explication a été rédigée sans lui
 * @param homeGamesPlayed matchs joués cette saison par l'équipe à domicile avant ce match (en dessous de 10, sa forme
 *                        repose surtout sur le bilan de la saison passée)
 * @param awayGamesPlayed même chose pour l'équipe à l'extérieur
 */
public record ExplainedPrediction(String homeTeam, String awayTeam, GameOutcomePrediction prediction,
                                  List<PredictionFactor> factors, Explanation explanation, boolean cached,
                                  boolean fallback, long homeGamesPlayed, long awayGamesPlayed) {
}
