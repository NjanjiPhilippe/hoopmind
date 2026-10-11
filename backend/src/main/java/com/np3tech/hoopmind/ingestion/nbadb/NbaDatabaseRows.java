package com.np3tech.hoopmind.ingestion.nbadb;

import java.time.LocalDate;
import java.util.List;

/**
 * Lignes lues dans la base NBA (schéma en étoile de nbadb : {@code dim_team}, {@code dim_game},
 * {@code fact_game_result}, {@code fact_team_game}, {@code fact_standings}).
 */
final class NbaDatabaseRows {

    private NbaDatabaseRows() {
    }

    record TeamRow(long teamId, String abbreviation, String fullName, String city, String arena,
                   Integer yearFounded, String conference, String division) {
    }

    /**
     * Un match de saison régulière ou de playoffs. Les scores sont {@code null} tant que le match n'est pas joué.
     * Les listes de points par période contiennent les quatre quart-temps puis les prolongations jouées.
     */
    record GameRow(String gameId, LocalDate gameDate, String seasonYear, long homeTeamId, long awayTeamId,
                   String arena, Integer homePoints, Integer awayPoints,
                   List<Integer> homePeriods, List<Integer> awayPeriods) {

        boolean played() {
            return homePoints != null && awayPoints != null;
        }

        /** Les identifiants stats.nba.com commencent par 002 en saison régulière et 004 en playoffs. */
        boolean playoffs() {
            return gameId.startsWith("004");
        }

        /** En playoffs, le huitième caractère donne le tour : 4 pour les Finales. */
        boolean finals() {
            return playoffs() && gameId.length() >= 8 && gameId.charAt(7) == '4';
        }

        int overtimePeriods() {
            return Math.max(0, Math.max(homePeriods.size(), awayPeriods.size()) - 4);
        }
    }

    record TeamGameRow(String gameId, long teamId, Integer points,
                       Integer fgm, Integer fga, Integer fg3m, Integer fg3a, Integer ftm, Integer fta,
                       Integer oreb, Integer dreb, Integer reb, Integer ast, Integer stl, Integer blk,
                       Integer tov, Integer pf) {
    }

    record StandingRow(String seasonYear, long teamId, Integer conferenceRank, Integer divisionRank) {
    }
}
