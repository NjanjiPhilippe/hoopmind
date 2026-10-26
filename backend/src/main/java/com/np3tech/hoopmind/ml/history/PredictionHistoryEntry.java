package com.np3tech.hoopmind.ml.history;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Une ligne de « Mes prédictions ».
 *
 * @param predictedHomePoints {@code null} si le modèle de score n'était pas encore entraîné
 * @param actualHomePoints    score réel de l'équipe à domicile, {@code null} tant que le match n'est pas joué (ou
 *                            s'il n'existe pas dans le calendrier à cette date)
 * @param correct             le vainqueur prédit est le bon ; {@code null} tant que le match n'est pas joué
 */
@Schema(requiredProperties = {"id", "seasonId", "homeTeamId", "homeTeam", "awayTeamId", "awayTeam", "gameDate", "playoff",
        "homeWinProbability", "model", "predictedAt"})
public record PredictionHistoryEntry(long id, long seasonId, long homeTeamId, String homeTeam, long awayTeamId,
                                     String awayTeam, LocalDate gameDate, boolean playoff, double homeWinProbability,
                                     Integer predictedHomePoints, Integer predictedAwayPoints, String model,
                                     Instant predictedAt, Integer actualHomePoints, Integer actualAwayPoints,
                                     Boolean correct) {
}
