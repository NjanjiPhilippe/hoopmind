package com.np3tech.hoopmind.ml;

/**
 * @param model identifiant du modèle qui a produit la prédiction
 * @param score score prédit, {@code null} tant que le modèle n'a pas été réentraîné depuis l'ajout du modèle de score
 */
public record GameOutcomePrediction(boolean homeTeamWins, double homeWinProbability, double awayWinProbability,
                                    String model, ScorePrediction score) {

    /** Prédiction du vainqueur seul, sans score. */
    public GameOutcomePrediction(boolean homeTeamWins, double homeWinProbability, double awayWinProbability, String model) {
        this(homeTeamWins, homeWinProbability, awayWinProbability, model, null);
    }
}
