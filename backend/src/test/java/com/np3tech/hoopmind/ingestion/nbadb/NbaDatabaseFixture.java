package com.np3tech.hoopmind.ingestion.nbadb;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Petite base SQLite au format nbadb (mêmes tables et colonnes que {@code nba.sqlite} de Wyatt Walsh),
 * pour tester l'import sans télécharger les plusieurs Go de la vraie base.
 */
final class NbaDatabaseFixture implements AutoCloseable {

    static final long BOS = 1610612738L;
    static final long LAL = 1610612747L;
    static final long NYK = 1610612752L;
    /** Une équipe absente de dim_team : ses matchs doivent être ignorés. */
    static final long UNKNOWN = 1610610000L;

    private final Connection connection;

    NbaDatabaseFixture(Path file) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
        try (Statement s = connection.createStatement()) {
            s.execute("""
                    CREATE TABLE dim_team (team_id INTEGER, abbreviation TEXT, full_name TEXT, city TEXT, state TEXT,
                        arena TEXT, year_founded INTEGER, conference TEXT, division TEXT)""");
            s.execute("""
                    CREATE TABLE dim_game (game_id TEXT, game_date TEXT, season_year TEXT, season_type TEXT,
                        home_team_id INTEGER, visitor_team_id INTEGER, matchup TEXT, arena_name TEXT, arena_city TEXT)""");
            s.execute("""
                    CREATE TABLE fact_game_result (game_id TEXT, game_date TEXT, season_year TEXT, season_type TEXT,
                        home_team_id INTEGER, visitor_team_id INTEGER, wl_home TEXT, pts_home INTEGER, pts_away INTEGER,
                        plus_minus_home REAL, plus_minus_away REAL,
                        pts_qtr1_home INTEGER, pts_qtr2_home INTEGER, pts_qtr3_home INTEGER, pts_qtr4_home INTEGER,
                        pts_ot1_home INTEGER, pts_ot2_home INTEGER,
                        pts_qtr1_away INTEGER, pts_qtr2_away INTEGER, pts_qtr3_away INTEGER, pts_qtr4_away INTEGER,
                        pts_ot1_away INTEGER, pts_ot2_away INTEGER)""");
            s.execute("""
                    CREATE TABLE fact_team_game (game_id TEXT, team_id INTEGER, fgm INTEGER, fga INTEGER, fg_pct REAL,
                        fg3m INTEGER, fg3a INTEGER, fg3_pct REAL, ftm INTEGER, fta INTEGER, ft_pct REAL,
                        oreb INTEGER, dreb INTEGER, reb INTEGER, ast INTEGER, stl INTEGER, blk INTEGER, tov INTEGER,
                        pf INTEGER, pts INTEGER, pts_qtr1 INTEGER, pts_qtr2 INTEGER, pts_qtr3 INTEGER, pts_qtr4 INTEGER,
                        season_year TEXT)""");
            s.execute("""
                    CREATE TABLE fact_standings (team_id INTEGER, conference TEXT, division TEXT, conf_rank INTEGER,
                        div_rank INTEGER, wins INTEGER, losses INTEGER, win_pct REAL, home_record TEXT, road_record TEXT,
                        last_ten TEXT, current_streak TEXT, games_back REAL, clinch_indicator TEXT, pts_pg REAL,
                        opp_pts_pg REAL, diff_pts_pg REAL, season_year TEXT, season_type TEXT)""");
        }
        team(BOS, "BOS", "Boston Celtics", "Boston", "East", "Atlantic");
        team(LAL, "LAL", "Los Angeles Lakers", "Los Angeles", "West", "Pacific");
        team(NYK, "NYK", "New York Knicks", "New York", "East", "Atlantic");
    }

    void team(long id, String abbreviation, String fullName, String city, String conference, String division) throws SQLException {
        insert("INSERT INTO dim_team VALUES (?, ?, ?, ?, NULL, 'Arena', 1946, ?, ?)",
                id, abbreviation, fullName, city, conference, division);
    }

    /** Un match programmé, sans résultat. */
    void scheduled(String gameId, String date, String season, long home, long away) throws SQLException {
        insert("INSERT INTO dim_game VALUES (?, ?, ?, NULL, ?, ?, NULL, 'Arena', NULL)", gameId, date, season, home, away);
    }

    /**
     * Un match joué, avec une feuille de match simple : pour 100 points, 88 tirs tentés, 10 rebonds offensifs,
     * 12 balles perdues et 20 lancers francs tentés, soit 98,8 possessions.
     */
    void played(String gameId, String date, String season, long home, long away, int homePoints, int awayPoints)
            throws SQLException {
        scheduled(gameId, date, season, home, away);
        insert("""
                INSERT INTO fact_game_result VALUES (?, ?, ?, NULL, ?, ?, ?, ?, ?, NULL, NULL,
                    ?, ?, ?, ?, NULL, NULL, ?, ?, ?, ?, NULL, NULL)""",
                gameId, date, season, home, away, homePoints > awayPoints ? "W" : "L", homePoints, awayPoints,
                homePoints / 4, homePoints / 4, homePoints / 4, homePoints - 3 * (homePoints / 4),
                awayPoints / 4, awayPoints / 4, awayPoints / 4, awayPoints - 3 * (awayPoints / 4));
        boxScore(gameId, home, homePoints, season);
        boxScore(gameId, away, awayPoints, season);
    }

    private void boxScore(String gameId, long team, int points, String season) throws SQLException {
        insert("""
                INSERT INTO fact_team_game VALUES (?, ?, 40, 88, 0.455, 12, 34, 0.353, 16, 20, 0.8,
                    10, 34, 44, 25, 7, 5, 12, 19, ?, NULL, NULL, NULL, NULL, ?)""", gameId, team, points, season);
    }

    void standing(String season, long team, int conferenceRank, int divisionRank) throws SQLException {
        insert("""
                INSERT INTO fact_standings (team_id, conf_rank, div_rank, season_year, season_type)
                VALUES (?, ?, ?, ?, 'Regular Season')""", team, conferenceRank, divisionRank, season);
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
