package com.np3tech.hoopmind.ml.history;

import java.time.LocalDate;

/** Le match tel que l'utilisateur l'a demandé : deux équipes, une saison, une date. */
public record PredictedMatch(long seasonId, long homeTeamId, long awayTeamId, LocalDate date, boolean playoff) {
}
