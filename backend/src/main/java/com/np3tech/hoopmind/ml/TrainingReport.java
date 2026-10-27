package com.np3tech.hoopmind.ml;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Résultat d'un entraînement du modèle de prédiction des matchs, tel qu'il est montré à l'administrateur.
 *
 * @param homeTeamBaseline     précision de la règle « l'équipe à domicile gagne » sur le même jeu de test
 * @param betterRecordBaseline précision de la règle « l'équipe au meilleur bilan gagne »
 * @param featureWeights       poids de chaque variable (échelle standardisée), du plus influent au moins influent
 * @param score                évaluation du modèle de score entraîné en même temps, {@code null} pour un entraînement
 *                             antérieur à son ajout
 */
public record TrainingReport(String model, String version, LocalDateTime trainedAt, Integer seasonsUsed,
                             String testSeason, Long trainingGames, long testGames, double accuracy, Double auc,
                             Double logLoss, Double precision, Double recall, Double f1, Double homeTeamBaseline,
                             Double betterRecordBaseline, Map<String, Long> confusionMatrix,
                             Map<String, Double> featureWeights, ScoreReport score) {

    /** Rapport du modèle du vainqueur seul. */
    public TrainingReport(String model, String version, LocalDateTime trainedAt, Integer seasonsUsed,
                          String testSeason, Long trainingGames, long testGames, double accuracy, Double auc,
                          Double logLoss, Double precision, Double recall, Double f1, Double homeTeamBaseline,
                          Double betterRecordBaseline, Map<String, Long> confusionMatrix,
                          Map<String, Double> featureWeights) {
        this(model, version, trainedAt, seasonsUsed, testSeason, trainingGames, testGames, accuracy, auc, logLoss,
                precision, recall, f1, homeTeamBaseline, betterRecordBaseline, confusionMatrix, featureWeights, null);
    }

    /**
     * Évaluation du modèle de score sur la même saison de test, en points.
     *
     * @param winnerAccuracy                  part des matchs où l'écart prédit désigne le bon vainqueur
     * @param pointsMeanAbsoluteError         erreur moyenne sur les points d'une équipe
     * @param marginMeanAbsoluteError         erreur moyenne sur l'écart
     * @param marginRootMeanSquaredError      erreur quadratique moyenne sur l'écart (pénalise les grosses erreurs)
     * @param totalMeanAbsoluteError          erreur moyenne sur le total de points du match
     * @param baselineMarginMeanAbsoluteError erreur moyenne sur l'écart de la règle « chaque équipe marque la moyenne
     *                                        de ce qu'elle marque et de ce que l'adversaire encaisse »
     */
    public record ScoreReport(String model, long testGames, double winnerAccuracy, Double pointsMeanAbsoluteError,
                              double marginMeanAbsoluteError, Double marginRootMeanSquaredError,
                              Double totalMeanAbsoluteError, Double baselineMarginMeanAbsoluteError) {
    }
}
