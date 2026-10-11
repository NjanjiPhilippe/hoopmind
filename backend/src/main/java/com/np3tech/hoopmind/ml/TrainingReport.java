package com.np3tech.hoopmind.ml;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Résultat d'un entraînement du modèle de prédiction des matchs, tel qu'il est montré à l'administrateur.
 *
 * @param homeTeamBaseline     précision de la règle « l'équipe à domicile gagne » sur le même jeu de test
 * @param betterRecordBaseline précision de la règle « l'équipe au meilleur bilan gagne »
 * @param featureWeights       poids de chaque variable (échelle standardisée), du plus influent au moins influent
 */
public record TrainingReport(String model, String version, LocalDateTime trainedAt, Integer seasonsUsed,
                             String testSeason, Long trainingGames, long testGames, double accuracy, Double auc,
                             Double logLoss, Double precision, Double recall, Double f1, Double homeTeamBaseline,
                             Double betterRecordBaseline, Map<String, Long> confusionMatrix,
                             Map<String, Double> featureWeights) {
}
