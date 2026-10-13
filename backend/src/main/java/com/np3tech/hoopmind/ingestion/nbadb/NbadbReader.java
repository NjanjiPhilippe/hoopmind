package com.np3tech.hoopmind.ingestion.nbadb;

import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.GameRow;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.StandingRow;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.TeamGameRow;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.TeamRow;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;

/**
 * Schéma en étoile de nbadb : {@code dim_team}, {@code dim_game}, {@code fact_game_result}, {@code fact_team_game},
 * {@code fact_standings}.
 */
final class NbadbReader extends NbaDatabaseReader {

    /** Saison régulière (002), play-in (005) et playoffs (004) ; présaison, All-Star et finale de la NBA Cup sont ignorés. */
    private static final String GAME_KINDS = "substr(g.game_id, 1, 3) IN ('002', '005', '004')";

    NbadbReader(Connection connection) {
        super(connection);
    }

    @Override
    public List<String> recentSeasons(int count) {
        List<String> seasons = query("""
                SELECT DISTINCT g.season_year FROM dim_game g
                WHERE substr(g.game_id, 1, 3) = '002' AND g.season_year IS NOT NULL
                ORDER BY g.season_year DESC LIMIT ?""", rs -> rs.getString(1), count);
        Collections.reverse(seasons);
        return seasons;
    }

    @Override
    public List<TeamRow> teams() {
        return query("""
                SELECT team_id, abbreviation, full_name, city, arena, year_founded, conference, division
                FROM dim_team""", rs -> new TeamRow(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                rs.getString(5), integer(rs, 6), rs.getString(7), rs.getString(8)));
    }

    @Override
    public List<GameRow> games(List<String> seasons) {
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
                        rs.getString(6), integer(rs, 7), integer(rs, 8), periods(rs, 9, 2), periods(rs, 15, 2)),
                seasons.toArray());
        return distinctBy(rows, GameRow::gameId);
    }

    @Override
    public List<TeamGameRow> teamGames(List<String> seasons) {
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

    @Override
    public List<StandingRow> standings(List<String> seasons) {
        return query("""
                SELECT season_year, team_id, MIN(conf_rank), MIN(div_rank)
                FROM fact_standings
                WHERE (season_type IS NULL OR season_type = 'Regular Season') AND season_year IN (%s)
                GROUP BY season_year, team_id""".formatted(placeholders(seasons)),
                rs -> new StandingRow(rs.getString(1), rs.getLong(2), integer(rs, 3), integer(rs, 4)),
                seasons.toArray());
    }
}
