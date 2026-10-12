package com.np3tech.hoopmind.ml.explanation;

import com.np3tech.hoopmind.ml.GameOutcomeAnalysis;

import java.time.LocalDate;

/** Tout ce que le LLM reçoit : le match et la prédiction détaillée, rien d'autre. */
public record ExplanationRequest(String homeTeam, String awayTeam, LocalDate date, boolean playoff,
                                 GameOutcomeAnalysis analysis) {
}
