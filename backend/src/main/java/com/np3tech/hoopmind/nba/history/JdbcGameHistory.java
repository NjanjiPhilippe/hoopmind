package com.np3tech.hoopmind.nba.history;

import com.np3tech.hoopmind.nba.GameHistory;
import com.np3tech.hoopmind.nba.GameRecord;
import com.np3tech.hoopmind.nba.GameRecord.BoxScore;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Lit l'historique des matchs en une seule requête (matchs, résultats et feuilles de match des deux équipes).
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
class JdbcGameHistory implements GameHistory {

    private static final String GAMES = """
            SELECT g.id, s.name AS season, s.year, CAST(g.scheduled_date AS DATE) AS game_date, g.game_type,
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
            WHERE g.game_type IN ('REGULAR_SEASON', 'PLAYOFFS', 'FINALS') AND s.year = ANY (?)
            ORDER BY game_date, g.id
            """;

    private final JdbcTemplate jdbc;

    @Override
    public List<Integer> recentSeasonYears(int count) {
        List<Integer> years = jdbc.queryForList("""
                SELECT DISTINCT s.year FROM seasons s
                JOIN games g ON g.season_id = s.id JOIN game_results r ON r.game_id = g.id
                ORDER BY s.year DESC LIMIT ?""", Integer.class, count);
        return years.reversed();
    }

    @Override
    public List<GameRecord> games(Collection<Integer> seasonYears) {
        if (seasonYears.isEmpty()) {
            return List.of();
        }
        return jdbc.query(connection -> {
            var statement = connection.prepareStatement(GAMES);
            statement.setArray(1, connection.createArrayOf("integer", seasonYears.toArray()));
            return statement;
        }, (rs, row) -> game(rs));
    }

    @Override
    public Optional<Integer> seasonYear(long seasonId) {
        return jdbc.query("SELECT year FROM seasons WHERE id = ?", (rs, row) -> rs.getInt(1), seasonId)
                .stream().findFirst();
    }

    private static GameRecord game(ResultSet rs) throws SQLException {
        return new GameRecord(
                rs.getLong("id"),
                rs.getString("season"),
                rs.getInt("year"),
                rs.getDate("game_date").toLocalDate(),
                rs.getLong("home_team_id"),
                rs.getLong("away_team_id"),
                !"REGULAR_SEASON".equals(rs.getString("game_type")),
                rs.getObject("home_team_score", Integer.class),
                rs.getObject("away_team_score", Integer.class),
                boxScore(rs, "h_"),
                boxScore(rs, "a_"));
    }

    private static BoxScore boxScore(ResultSet rs, String prefix) throws SQLException {
        Integer fga = rs.getObject(prefix + "fga", Integer.class);
        if (fga == null) {
            return null;
        }
        return new BoxScore(fga, value(rs, prefix + "oreb"), value(rs, prefix + "tov"), value(rs, prefix + "fta"));
    }

    private static int value(ResultSet rs, String column) throws SQLException {
        Integer value = rs.getObject(column, Integer.class);
        return value == null ? 0 : value;
    }
}
