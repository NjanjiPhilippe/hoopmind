package com.np3tech.hoopmind.ingestion.nbadb;

import com.np3tech.hoopmind.ingestion.nbadb.StatsNbaClient.GameLogLine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Réponses de {@code leaguegamelog} telles que stats.nba.com les renvoie : colonnes dans {@code headers}, une ligne par
 * équipe et par match dans {@code rowSet}. Chaque équipe a la même feuille de match que dans {@link NbaDatabaseFixture}.
 */
final class StatsNbaFixture {

    static final long BOS = 1610612738L;
    static final long LAL = 1610612747L;
    static final long NYK = 1610612752L;
    static final long POR = 1610612757L;

    static final List<String> HEADERS = List.of("SEASON_ID", "TEAM_ID", "TEAM_ABBREVIATION", "TEAM_NAME", "GAME_ID",
            "GAME_DATE", "MATCHUP", "WL", "MIN", "FGM", "FGA", "FG_PCT", "FG3M", "FG3A", "FG3_PCT", "FTM", "FTA", "FT_PCT",
            "OREB", "DREB", "REB", "AST", "STL", "BLK", "TOV", "PF", "PTS", "PLUS_MINUS", "VIDEO_AVAILABLE");

    private static final Map<Long, String[]> TEAMS = Map.of(
            BOS, new String[]{"BOS", "Boston Celtics"},
            LAL, new String[]{"LAL", "Los Angeles Lakers"},
            NYK, new String[]{"NYK", "New York Knicks"},
            POR, new String[]{"POR", "Portland Trail Blazers"});

    private final List<List<Object>> rows = new ArrayList<>();

    /** Un match joué : deux lignes, l'affiche « vs. » pour l'équipe qui reçoit et « @ » pour l'autre. */
    StatsNbaFixture played(String gameId, String date, long home, long away, int homePoints, int awayPoints) {
        rows.add(row(gameId, date, home, abbreviation(home) + " vs. " + abbreviation(away), homePoints, awayPoints));
        rows.add(row(gameId, date, away, abbreviation(away) + " @ " + abbreviation(home), awayPoints, homePoints));
        return this;
    }

    /** Une ligne seule, sans l'adversaire. */
    StatsNbaFixture orphan(String gameId, String date, long team, long opponent, int points) {
        rows.add(row(gameId, date, team, abbreviation(team) + " vs. " + abbreviation(opponent), points, points - 1));
        return this;
    }

    private static List<Object> row(String gameId, String date, long team, String matchup, int points, int against) {
        List<Object> row = new ArrayList<>(List.of("22024", team, abbreviation(team), TEAMS.get(team)[1], gameId, date,
                matchup, points > against ? "W" : "L", 240, 40, 88, 0.455, 12, 34, 0.353, 16, 20, 0.8,
                10, 34, 44, 25, 7, 5, 12, 19, points, points - against));
        row.add(1);
        return row;
    }

    private static String abbreviation(long team) {
        return TEAMS.get(team)[0];
    }

    List<GameLogLine> lines() {
        return rows.stream().map(row -> {
            Map<String, Object> values = new LinkedHashMap<>();
            for (int i = 0; i < HEADERS.size(); i++) {
                values.put(HEADERS.get(i), row.get(i));
            }
            return new GameLogLine(values);
        }).toList();
    }

    /** Le corps JSON de la réponse. */
    String json() {
        String headers = HEADERS.stream().map(h -> "\"" + h + "\"").collect(Collectors.joining(","));
        String rowSet = rows.stream()
                .map(row -> row.stream().map(v -> v instanceof String s ? "\"" + s + "\"" : String.valueOf(v))
                        .collect(Collectors.joining(",", "[", "]")))
                .collect(Collectors.joining(","));
        return """
                {"resource":"leaguegamelog","parameters":{},"resultSets":[{"name":"LeagueGameLog","headers":[%s],"rowSet":[%s]}]}"""
                .formatted(headers, rowSet);
    }
}
