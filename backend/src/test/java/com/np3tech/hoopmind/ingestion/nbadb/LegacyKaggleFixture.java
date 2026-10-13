package com.np3tech.hoopmind.ingestion.nbadb;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Petite base SQLite à l'ancien format de Wyatt Walsh, celui encore livré dans l'archive Kaggle : tables {@code game},
 * {@code line_score}, {@code team}, {@code team_details} et {@code team_info_common} vide. Comme dans le vrai fichier,
 * les points sont des {@code REAL} et les identifiants du texte ; un match a un identifiant numérique, sans ses zéros.
 */
final class LegacyKaggleFixture implements AutoCloseable {

    static final long BOS = 1610612738L;
    static final long LAL = 1610612747L;
    static final long NYK = 1610612752L;
    static final long PHI = 1610612755L;

    private final Connection connection;

    LegacyKaggleFixture(Path file) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
        try (Statement s = connection.createStatement()) {
            s.execute("CREATE TABLE team (id TEXT, full_name TEXT, abbreviation TEXT, nickname TEXT, city TEXT, state TEXT, year_founded REAL)");
            s.execute("""
                    CREATE TABLE team_details (team_id TEXT, abbreviation TEXT, nickname TEXT, yearfounded REAL, city TEXT,
                        arena TEXT, arenacapacity REAL, owner TEXT, generalmanager TEXT, headcoach TEXT)""");
            s.execute("""
                    CREATE TABLE team_info_common (team_id TEXT, season_year TEXT, team_conference TEXT, team_division TEXT,
                        conf_rank REAL, div_rank REAL)""");
            s.execute("""
                    CREATE TABLE game (season_id TEXT, team_id_home TEXT, team_abbreviation_home TEXT, game_id TEXT,
                        game_date TIMESTAMP, wl_home TEXT, fgm_home REAL, fga_home REAL, fg3m_home REAL, fg3a_home REAL,
                        ftm_home REAL, fta_home REAL, oreb_home REAL, dreb_home REAL, reb_home REAL, ast_home REAL,
                        stl_home REAL, blk_home REAL, tov_home REAL, pf_home REAL, pts_home REAL,
                        team_id_away TEXT, team_abbreviation_away TEXT, wl_away TEXT, fgm_away REAL, fga_away REAL,
                        fg3m_away REAL, fg3a_away REAL, ftm_away REAL, fta_away REAL, oreb_away REAL, dreb_away REAL,
                        reb_away REAL, ast_away REAL, stl_away REAL, blk_away REAL, tov_away REAL, pf_away REAL,
                        pts_away REAL, season_type TEXT)""");
            s.execute("""
                    CREATE TABLE line_score (game_date_est TIMESTAMP, game_id TEXT, team_id_home TEXT,
                        pts_qtr1_home REAL, pts_qtr2_home REAL, pts_qtr3_home REAL, pts_qtr4_home REAL,
                        pts_ot1_home REAL, pts_ot2_home REAL, pts_ot3_home REAL, pts_ot4_home REAL, pts_ot5_home REAL,
                        pts_ot6_home REAL, pts_ot7_home REAL, pts_ot8_home REAL, pts_ot9_home REAL, pts_ot10_home REAL,
                        pts_home REAL, team_id_away TEXT,
                        pts_qtr1_away REAL, pts_qtr2_away REAL, pts_qtr3_away REAL, pts_qtr4_away REAL,
                        pts_ot1_away REAL, pts_ot2_away REAL, pts_ot3_away REAL, pts_ot4_away REAL, pts_ot5_away REAL,
                        pts_ot6_away REAL, pts_ot7_away REAL, pts_ot8_away REAL, pts_ot9_away REAL, pts_ot10_away REAL,
                        pts_away REAL)""");
        }
        team(BOS, "BOS", "Boston Celtics", "Boston", "TD Garden");
        team(LAL, "LAL", "Los Angeles Lakers", "Los Angeles", "Crypto.com Arena");
        team(NYK, "NYK", "New York Knicks", "New York", "Madison Square Garden");
        team(PHI, "PHI", "Philadelphia 76ers", "Philadelphia", "Wells Fargo Center");
    }

    private void team(long id, String abbreviation, String fullName, String city, String arena) throws SQLException {
        insert("INSERT INTO team VALUES (?, ?, ?, NULL, ?, NULL, 1946.0)", String.valueOf(id), fullName, abbreviation, city);
        insert("INSERT INTO team_details (team_id, abbreviation, arena) VALUES (?, ?, ?)", String.valueOf(id), abbreviation, arena);
    }

    /**
     * Un match joué. {@code seasonId} suit le format Kaggle (« 22019 » : saison régulière 2019-20). Chaque équipe a la
     * même feuille de match que dans {@link NbaDatabaseFixture} ; les points par période vont dans {@code line_score}.
     */
    void played(Object gameId, String seasonId, String date, long home, long away, int homePoints, int awayPoints,
                String seasonType) throws SQLException {
        insert("""
                INSERT INTO game VALUES (?, ?, NULL, ?, ?, ?, 40.0, 88.0, 12.0, 34.0, 16.0, 20.0, 10.0, 34.0, 44.0, 25.0,
                    7.0, 5.0, 12.0, 19.0, ?, ?, NULL, ?, 40.0, 88.0, 12.0, 34.0, 16.0, 20.0, 10.0, 34.0, 44.0, 25.0, 7.0,
                    5.0, 12.0, 19.0, ?, ?)""",
                seasonId, String.valueOf(home), gameId, date + " 00:00:00", homePoints > awayPoints ? "W" : "L",
                (double) homePoints, String.valueOf(away), homePoints > awayPoints ? "L" : "W", (double) awayPoints,
                seasonType);
    }

    /** Points par période : quatre quart-temps puis les prolongations. */
    void lineScore(Object gameId, int[] home, int[] away) throws SQLException {
        Object[] values = new Object[31];
        values[0] = gameId;
        for (int i = 0; i < 14; i++) {
            values[1 + i] = i < home.length ? (double) home[i] : null;
            values[15 + i] = i < away.length ? (double) away[i] : null;
        }
        values[29] = (double) java.util.Arrays.stream(home).sum();
        values[30] = (double) java.util.Arrays.stream(away).sum();
        insert("""
                INSERT INTO line_score (game_id,
                    pts_qtr1_home, pts_qtr2_home, pts_qtr3_home, pts_qtr4_home, pts_ot1_home, pts_ot2_home, pts_ot3_home,
                    pts_ot4_home, pts_ot5_home, pts_ot6_home, pts_ot7_home, pts_ot8_home, pts_ot9_home, pts_ot10_home,
                    pts_qtr1_away, pts_qtr2_away, pts_qtr3_away, pts_qtr4_away, pts_ot1_away, pts_ot2_away, pts_ot3_away,
                    pts_ot4_away, pts_ot5_away, pts_ot6_away, pts_ot7_away, pts_ot8_away, pts_ot9_away, pts_ot10_away,
                    pts_home, pts_away)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", values);
    }

    private void insert(String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                statement.setObject(i + 1, values[i]);
            }
            statement.executeUpdate();
        }
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}
