package com.np3tech.hoopmind.ml;

/**
 * @param model identifiant du modèle qui a produit la prédiction
 */
public record GameOutcomePrediction(boolean homeTeamWins, double homeWinProbability, double awayWinProbability,
                                    String model) {
}
