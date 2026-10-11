package com.np3tech.hoopmind.ml.spark;

import lombok.RequiredArgsConstructor;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Charge les matchs importés (une ligne par équipe et par match) dans un DataFrame Spark au format
 * {@link GameFeatureBuilder#TEAM_GAME_SCHEMA}.
 */
@Component
@ConditionalOnSpark
@RequiredArgsConstructor
class TeamGameLoader {

    private static final String GAMES = """
            SELECT g.id, s.name AS season, CAST(g.scheduled_date AS DATE) AS game_date, g.game_type,
                   g.home_team_id, g.away_team_id, r.home_team_score, r.away_team_score,
                   h.field_goals_attempted AS h_fga, h.offensive_rebounds AS h_oreb, h.turnovers AS h_tov,
                   h.free_throws_attempted AS h_fta,
                   a.field_goals_attempted AS a_fga, a.offensive_rebounds AS a_oreb, a.turnovers AS a_tov,
                   a.free_throws_attempted AS a_fta
            FROM games g
            JOIN seasons s ON s.id = g.season_id
            LEFT JOIN game_results r ON r.game_id = g.id
            LEFT JOIN team_game_stats h ON h.game_id = g.id AND h.team_id = g.home_team_id
            LEFT JOIN team_game_stats a ON a.game_id = g.id AND a.team_id = g.away_team_id
            WHERE g.game_type IN ('REGULAR_SEASON', 'PLAYOFFS', 'FINALS') AND s.year IN (%s)
            """;

    private final JdbcTemplate jdbc;
    private final SparkSession spark;

    /** Les {@code count} saisons les plus récentes qui ont des matchs joués, de la plus ancienne à la plus récente. */
    List<Integer> recentSeasonYears(int count) {
        List<Integer> years = jdbc.queryForList("""
                SELECT DISTINCT s.year FROM seasons s
                JOIN games g ON g.season_id = s.id JOIN game_results r ON r.game_id = g.id
                ORDER BY s.year DESC LIMIT ?""", Integer.class, count);
        return years.reversed();
    }

    Dataset<Row> teamGames(List<Integer> seasonYears) {
        if (seasonYears.isEmpty()) {
            return spark.createDataFrame(List.of(), GameFeatureBuilder.TEAM_GAME_SCHEMA);
        }
        String placeholders = String.join(", ", seasonYears.stream().map(y -> "?").toList());
        List<Row> rows = new ArrayList<>();
        jdbc.query(GAMES.formatted(placeholders), rs -> {
            rows.add(teamRow(rs, true));
            rows.add(teamRow(rs, false));
        }, seasonYears.toArray());
        return spark.createDataFrame(rows, GameFeatureBuilder.TEAM_GAME_SCHEMA);
    }

    private static Row teamRow(ResultSet rs, boolean home) throws SQLException {
        Integer homeScore = (Integer) rs.getObject("home_team_score");
        Integer awayScore = (Integer) rs.getObject("away_team_score");
        Integer scored = home ? homeScore : awayScore;
        Integer allowed = home ? awayScore : homeScore;
        Double own = possessions(rs, home ? "h_" : "a_");
        Double opponent = possessions(rs, home ? "a_" : "h_");
        return RowFactory.create(
                rs.getLong("id"),
                rs.getString("season"),
                rs.getDate("game_date"),
                rs.getLong(home ? "home_team_id" : "away_team_id"),
                home ? 1 : 0,
                "REGULAR_SEASON".equals(rs.getString("game_type")) ? 0 : 1,
                scored == null || allowed == null ? null : scored > allowed ? 1 : 0,
                scored,
                allowed,
                own == null || opponent == null ? null : own,
                own == null || opponent == null ? null : opponent);
    }

    /** Possessions estimées : FGA - OREB + TOV + 0,44 × FTA. */
    private static Double possessions(ResultSet rs, String prefix) throws SQLException {
        Integer fga = (Integer) rs.getObject(prefix + "fga");
        if (fga == null) {
            return null;
        }
        return fga - value(rs, prefix + "oreb") + value(rs, prefix + "tov") + 0.44 * value(rs, prefix + "fta");
    }

    private static int value(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        return value == null ? 0 : ((Number) value).intValue();
    }
}
