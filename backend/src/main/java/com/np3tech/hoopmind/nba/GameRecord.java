package com.np3tech.hoopmind.nba;

import java.time.LocalDate;

/**
 * Un match de saison régulière, de play-in ou de playoffs, tel que le module NBA l'expose aux autres modules
 * (en lecture seule, sans entité JPA).
 *
 * @param homePoints {@code null} tant que le match n'est pas joué
 * @param home       feuille de match de l'équipe à domicile, {@code null} si elle manque
 */
public record GameRecord(long gameId, String season, int seasonYear, LocalDate date, long homeTeamId,
                         long awayTeamId, boolean playoff, Integer homePoints, Integer awayPoints,
                         BoxScore home, BoxScore away) {

    public boolean played() {
        return homePoints != null && awayPoints != null;
    }

    /**
     * Les quelques colonnes de la feuille de match qui servent à estimer les possessions.
     */
    public record BoxScore(int fieldGoalsAttempted, int offensiveRebounds, int turnovers, int freeThrowsAttempted) {

        /** Possessions estimées par la formule usuelle : FGA - OREB + TOV + 0,44 × FTA. */
        public double possessions() {
            return fieldGoalsAttempted - offensiveRebounds + turnovers + 0.44 * freeThrowsAttempted;
        }
    }
}
