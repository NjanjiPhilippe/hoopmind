package com.np3tech.hoopmind.ingestion.nbadb;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Écrit {@code Games.csv} et {@code TeamStatistics.csv} au format du jeu Kaggle d'Eoin Moore (en-têtes de la version
 * 515), avec les identifiants stockés en nombre comme dans les vrais fichiers.
 */
final class EoinMooreFixture {

    static final long BOS = 1610612738L;
    static final long NYK = 1610612752L;
    static final long LAL = 1610612747L;
    static final long POR = 1610612757L;

    private static final String GAMES_HEADER = "gameId,gameDateTimeEst,hometeamCity,hometeamName,hometeamId,"
            + "awayteamCity,awayteamName,awayteamId,homeScore,awayScore,winner,gameType,gameSubtype,gameLabel,"
            + "gameSubLabel,seriesGameNumber,attendance,arenaId,arenaName,arenaCity,arenaState,officials,gameDate";

    private static final String TEAM_HEADER = "gameId,gameDateTimeEst,teamCity,teamName,teamId,opponentTeamCity,"
            + "opponentTeamName,opponentTeamId,home,win,teamScore,opponentScore,assists,blocks,steals,"
            + "fieldGoalsAttempted,fieldGoalsMade,fieldGoalsPercentage,threePointersAttempted,threePointersMade,"
            + "threePointersPercentage,freeThrowsAttempted,freeThrowsMade,freeThrowsPercentage,reboundsDefensive,"
            + "reboundsOffensive,reboundsTotal,foulsPersonal,turnovers,plusMinusPoints,numMinutes,q1Points,q2Points,"
            + "q3Points,q4Points,benchPoints,biggestLead,biggestScoringRun,leadChanges,pointsFastBreak,"
            + "pointsFromTurnovers,pointsInThePaint,pointsSecondChance,timesTied,timeoutsRemaining,seasonWins,"
            + "seasonLosses,coachId,gameType,gameLabel,gameSubLabel,seriesGameNumber,seed,reboundsTeam,turnoversTeam,"
            + "ot1Points,ot2Points,otAllPoints,gameDate";

    private final List<String> games = new ArrayList<>();
    private final List<String> teamLines = new ArrayList<>();

    /**
     * Un match joué. {@code homeQuarters} et {@code awayQuarters} : quatre quart-temps puis les prolongations.
     *
     * @param gameId identifiant tel qu'il figure dans les fichiers (« 21000001 », sans les zéros de tête)
     */
    EoinMooreFixture played(String gameId, String date, long home, long away, int[] homeQuarters, int[] awayQuarters) {
        int homeScore = sum(homeQuarters);
        int awayScore = sum(awayQuarters);
        games.add(String.join(",", gameId, date + " 19:30:00", city(home), name(home), String.valueOf(home),
                city(away), name(away), String.valueOf(away), homeScore + ".0", awayScore + ".0",
                String.valueOf(homeScore > awayScore ? home : away), "Regular Season", "", "", "", "", "18624",
                "1", "\"TD Garden\"", "Boston", "MA", "\"Ref A, Ref B\"", date));
        teamLines.add(teamLine(gameId, date, home, away, true, homeQuarters, awayScore));
        teamLines.add(teamLine(gameId, date, away, home, false, awayQuarters, homeScore));
        return this;
    }

    /** Un match au calendrier, sans score ni feuille de match. */
    EoinMooreFixture scheduled(String gameId, String date, long home, long away) {
        games.add(String.join(",", gameId, date + " 19:30:00", city(home), name(home), String.valueOf(home),
                city(away), name(away), String.valueOf(away), "", "", "", "Regular Season", "", "", "", "", "", "", "",
                "", "", "", date));
        return this;
    }

    void write(Path directory) throws IOException {
        Files.createDirectories(directory);
        // Les fichiers téléchargés depuis Kaggle commencent parfois par une marque d'ordre des octets.
        Files.writeString(directory.resolve(EoinMooreSource.GAMES_FILE),
                "﻿" + GAMES_HEADER + "\n" + String.join("\n", games) + "\n", StandardCharsets.UTF_8);
        Files.writeString(directory.resolve(EoinMooreSource.TEAM_STATISTICS_FILE),
                TEAM_HEADER + "\n" + String.join("\n", teamLines) + "\n", StandardCharsets.UTF_8);
    }

    private static String teamLine(String gameId, String date, long team, long opponent, boolean home, int[] quarters,
                                   int opponentScore) {
        int score = sum(quarters);
        int overtimes = quarters.length - 4;
        int ot1 = overtimes >= 1 ? quarters[4] : 0;
        int ot2 = overtimes >= 2 ? quarters[5] : 0;
        int otAll = score - quarters[0] - quarters[1] - quarters[2] - quarters[3];
        return String.join(",", gameId, date + " 19:30:00", city(team), name(team), String.valueOf(team),
                city(opponent), name(opponent), String.valueOf(opponent), home ? "1" : "0",
                score > opponentScore ? "1" : "0", score + ".0", opponentScore + ".0",
                "25", "5", "8", "88", "42", "0.477", "35", "13", "0.371", "20", "16", "0.8", "33", "10", "43", "19",
                "13", String.valueOf(score - opponentScore), String.valueOf(240 + 25 * overtimes),
                String.valueOf(quarters[0]), String.valueOf(quarters[1]), String.valueOf(quarters[2]),
                String.valueOf(quarters[3]), "30", "15", "10", "5", "12", "14", "48", "11", "3", "1", "1", "0", "",
                "Regular Season", "", "", "", "", "0", "0", String.valueOf(ot1), String.valueOf(ot2),
                String.valueOf(otAll), date);
    }

    private static int sum(int[] values) {
        int sum = 0;
        for (int value : values) {
            sum += value;
        }
        return sum;
    }

    private static String city(long team) {
        if (team == POR) return "Portland";
        if (team == LAL) return "Los Angeles";
        return team == BOS ? "Boston" : "New York";
    }

    private static String name(long team) {
        if (team == POR) return "Trail Blazers";
        if (team == LAL) return "Lakers";
        return team == BOS ? "Celtics" : "Knicks";
    }
}
