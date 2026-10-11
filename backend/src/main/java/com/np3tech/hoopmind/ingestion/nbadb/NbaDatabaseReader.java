package com.np3tech.hoopmind.ingestion.nbadb;

import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.GameRow;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.StandingRow;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.TeamGameRow;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.TeamRow;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Lecture du fichier {@code nba.sqlite} de nbadb, en lecture seule. Seules les tables utiles aux prédictions sont lues,
 * et seulement pour les saisons demandées.
 */
final class NbaDatabaseReader implements AutoCloseable {

    /** Saison régulière (002) et playoffs (004) ; présaison, All-Star, play-in et finale de la NBA Cup sont ignorés. */
    private static final String GAME_KINDS = "substr(g.game_id, 1, 3) IN ('002', '004')";

    private final Connection connection;

    private NbaDatabaseReader(Connection connection) {
        this.connection = connection;
    }

    static NbaDatabaseReader open(Path file) {
        if (!Files.isRegularFile(file)) {
            throw new NbaImportException("Base NBA introuvable : " + file.toAbsolutePath()
                    + ". Téléchargez-la avec scripts/download-nba-database.sh ou renseignez NBA_DATABASE_PATH.");
        }
        try {
            return new NbaDatabaseReader(DriverManager.getConnection("jdbc:sqlite:file:" + file.toAbsolutePath() + "?mode=ro"));
        } catch (SQLException e) {
            throw new NbaImportException("Impossible d'ouvrir la base NBA " + file + " : " + e.getMessage(), e);
        }
    }

    /** Les {@code count} saisons les plus récentes qui ont des matchs de saison régulière, de la plus ancienne à la plus récente. */
    List<String> recentSeasons(int count) {
        List<String> seasons = query("""
                SELECT DISTINCT g.season_year FROM dim_game g
                WHERE substr(g.game_id, 1, 3) = '002' AND g.season_year IS NOT NULL
                ORDER BY g.season_year DESC LIMIT ?""", rs -> rs.getString(1), count);
        Collections.reverse(seasons);
        return seasons;
    }

    List<TeamRow> teams() {
        return query("""
                SELECT team_id, abbreviation, full_name, city, arena, year_founded, conference, division
                FROM dim_team""", rs -> new TeamRow(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                rs.getString(5), integer(rs, 6), rs.getString(7), rs.getString(8)));
    }

    List<GameRow> games(List<String> seasons) {
        List<GameRow> rows = query("""
                SELECT g.game_id, g.game_date, g.season_year, g.home_team_id, g.visitor_team_id, g.arena_name,
                       r.pts_home, r.pts_away,
                       r.pts_qtr1_home, r.pts_qtr2_home, r.pts_qtr3_home, r.pts_qtr4_home, r.pts_ot1_home, r.pts_ot2_home,
                       r.pts_qtr1_away, r.pts_qtr2_away, r.pts_qtr3_away, r.pts_qtr4_away, r.pts_ot1_away, r.pts_ot2_away
                FROM dim_game g
                LEFT JOIN fact_game_result r ON r.game_id = g.game_id
                WHERE %s AND g.home_team_id IS NOT NULL AND g.visitor_team_id IS NOT NULL
                  AND g.season_year IN (%s)
                ORDER BY g.game_date, g.game_id""".formatted(GAME_KINDS, placeholders(seasons)),
                rs -> new GameRow(rs.getString(1), date(rs.getString(2)), rs.getString(3), rs.getLong(4), rs.getLong(5),
                        rs.getString(6), integer(rs, 7), integer(rs, 8), periods(rs, 9), periods(rs, 15)),
                seasons.toArray());
        return distinctBy(rows, GameRow::gameId);
    }

    List<TeamGameRow> teamGames(List<String> seasons) {
        List<TeamGameRow> rows = query("""
                SELECT t.game_id, t.team_id, t.pts, t.fgm, t.fga, t.fg3m, t.fg3a, t.ftm, t.fta,
                       t.oreb, t.dreb, t.reb, t.ast, t.stl, t.blk, t.tov, t.pf
                FROM fact_team_game t
                JOIN dim_game g ON g.game_id = t.game_id
                WHERE %s AND g.season_year IN (%s)""".formatted(GAME_KINDS, placeholders(seasons)),
                rs -> new TeamGameRow(rs.getString(1), rs.getLong(2), integer(rs, 3), integer(rs, 4), integer(rs, 5),
                        integer(rs, 6), integer(rs, 7), integer(rs, 8), integer(rs, 9), integer(rs, 10), integer(rs, 11),
                        integer(rs, 12), integer(rs, 13), integer(rs, 14), integer(rs, 15), integer(rs, 16), integer(rs, 17)),
                seasons.toArray());
        return distinctBy(rows, row -> row.gameId() + "/" + row.teamId());
    }

    List<StandingRow> standings(List<String> seasons) {
        return query("""
                SELECT season_year, team_id, MIN(conf_rank), MIN(div_rank)
                FROM fact_standings
                WHERE (season_type IS NULL OR season_type = 'Regular Season') AND season_year IN (%s)
                GROUP BY season_year, team_id""".formatted(placeholders(seasons)),
                rs -> new StandingRow(rs.getString(1), rs.getLong(2), integer(rs, 3), integer(rs, 4)),
                seasons.toArray());
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException ignored) {
            // lecture seule : rien à perdre
        }
    }

    private interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    private <T> List<T> query(String sql, RowMapper<T> mapper, Object... parameters) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            List<T> rows = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapper.map(rs));
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new NbaImportException("Lecture de la base NBA impossible (" + e.getMessage()
                    + "). Le fichier suit-il bien le schéma de nbadb ?", e);
        }
    }

    private static String placeholders(List<?> values) {
        return values.stream().map(v -> "?").collect(Collectors.joining(", "));
    }

    private static Integer integer(ResultSet rs, int column) throws SQLException {
        Object value = rs.getObject(column);
        return value == null ? null : ((Number) value).intValue();
    }

    /** Quatre quart-temps puis les prolongations jouées (une prolongation à 0 point reste une prolongation jouée). */
    private static List<Integer> periods(ResultSet rs, int firstColumn) throws SQLException {
        List<Integer> periods = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Integer points = integer(rs, firstColumn + i);
            if (points == null) {
                return List.of();
            }
            periods.add(points);
        }
        for (int i = 4; i < 6; i++) {
            Integer points = integer(rs, firstColumn + i);
            if (points != null) {
                periods.add(points);
            }
        }
        return periods;
    }

    /** Les dates arrivent en texte ISO, avec ou sans heure. */
    static LocalDate date(String value) {
        if (value == null || value.length() < 10) {
            throw new NbaImportException("Date de match illisible : " + value);
        }
        return LocalDate.parse(value.substring(0, 10));
    }

    private static <T> List<T> distinctBy(List<T> rows, java.util.function.Function<T, String> key) {
        Map<String, T> unique = new LinkedHashMap<>();
        rows.forEach(row -> unique.putIfAbsent(key.apply(row), row));
        return new ArrayList<>(unique.values());
    }
}
