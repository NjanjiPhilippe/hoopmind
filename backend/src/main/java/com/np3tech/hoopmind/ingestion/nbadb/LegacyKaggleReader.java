package com.np3tech.hoopmind.ingestion.nbadb;

import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.GameRow;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.StandingRow;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.TeamGameRow;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.TeamRow;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Ancien format de la base de Wyatt Walsh, encore livré dans l'archive Kaggle {@code wyattowalsh/basketball} :
 * une ligne par match dans {@code game} (scores et feuille de match des deux équipes), les points par période dans
 * {@code line_score}, les équipes dans {@code team} et {@code team_details}.
 * <p>
 * Ce format a trois différences avec nbadb, compensées ici :
 * <ul>
 *   <li>la saison est codée dans {@code season_id} (« 22023 » : type puis année de début) et devient « 2023-24 » ;</li>
 *   <li>les identifiants de match et d'équipe peuvent être stockés en nombre : ils sont normalisés
 *       (« 0022300001 » sur dix caractères) pour garder les préfixes 002 / 004 et les identifiants externes de nbadb ;</li>
 *   <li>{@code team_info_common}, qui donnait conférences, divisions et classements, est vide : conférences et divisions
 *       viennent de l'alignement actuel de la NBA (inchangé depuis 2004-05) et les classements sont recalculés à partir
 *       des résultats de saison régulière (pourcentage de victoires, sans les critères de départage officiels).</li>
 * </ul>
 */
final class LegacyKaggleReader extends NbaDatabaseReader {

    /** Identifiant de match sur dix caractères, quel que soit son type de stockage. */
    private static final String GAME_ID = "printf('%010d', CAST(game_id AS INTEGER))";

    /** Année de début de saison : « 22023 » (ou 22023.0) donne 2023. */
    private static final String START_YEAR = "CAST(substr(CAST(season_id AS TEXT), 2, 4) AS INTEGER)";

    /** Les matchs de {@code game} avec leur identifiant normalisé et leur année de saison. */
    private static final String GAMES = """
            WITH g AS (SELECT %s AS gid, %s AS start_year, * FROM game)""".formatted(GAME_ID, START_YEAR);

    /** Saison régulière (002) et playoffs (004) ; présaison, All-Star et play-in sont ignorés. */
    private static final String GAME_KINDS = "substr(g.gid, 1, 3) IN ('002', '004')";

    private static final int OVERTIME_COLUMNS = 10;

    /** Alignement de la NBA depuis 2004-05 : abréviation, conférence, division. */
    private static final Map<String, String[]> ALIGNMENT = alignment(
            "East", "Atlantic", "BOS", "BKN", "NYK", "PHI", "TOR",
            "East", "Central", "CHI", "CLE", "DET", "IND", "MIL",
            "East", "Southeast", "ATL", "CHA", "MIA", "ORL", "WAS",
            "West", "Northwest", "DEN", "MIN", "OKC", "POR", "UTA",
            "West", "Pacific", "GSW", "LAC", "LAL", "PHX", "SAC",
            "West", "Southwest", "DAL", "HOU", "MEM", "NOP", "SAS");

    LegacyKaggleReader(Connection connection) {
        super(connection);
    }

    @Override
    List<String> recentSeasons(int count) {
        List<String> seasons = query(GAMES + """
                SELECT DISTINCT g.start_year FROM g
                WHERE substr(g.gid, 1, 3) = '002' AND g.start_year IS NOT NULL
                ORDER BY g.start_year DESC LIMIT ?""", rs -> season(rs.getInt(1)), count);
        Collections.reverse(seasons);
        return seasons;
    }

    @Override
    List<TeamRow> teams() {
        return query("""
                SELECT CAST(t.id AS INTEGER), t.abbreviation, t.full_name, t.city, d.arena, t.year_founded
                FROM team t
                LEFT JOIN (SELECT CAST(team_id AS INTEGER) AS team_id, MAX(arena) AS arena FROM team_details
                           GROUP BY CAST(team_id AS INTEGER)) d ON d.team_id = CAST(t.id AS INTEGER)""",
                rs -> {
                    String abbreviation = rs.getString(2);
                    String[] alignment = ALIGNMENT.getOrDefault(abbreviation, new String[2]);
                    return new TeamRow(rs.getLong(1), abbreviation, rs.getString(3), rs.getString(4), rs.getString(5),
                            integer(rs, 6), alignment[0], alignment[1]);
                });
    }

    @Override
    List<GameRow> games(List<String> seasons) {
        Map<String, List<List<Integer>>> periods = periods();
        List<GameRow> rows = query(GAMES + """
                SELECT g.gid, g.game_date, g.start_year, CAST(g.team_id_home AS INTEGER), CAST(g.team_id_away AS INTEGER),
                       g.pts_home, g.pts_away
                FROM g
                WHERE %s AND g.team_id_home IS NOT NULL AND g.team_id_away IS NOT NULL AND g.start_year IN (%s)
                ORDER BY g.game_date, g.gid""".formatted(GAME_KINDS, placeholders(seasons)),
                rs -> {
                    String gameId = rs.getString(1);
                    List<List<Integer>> scores = periods.getOrDefault(gameId, List.of(List.of(), List.of()));
                    return new GameRow(gameId, date(rs.getString(2)), season(rs.getInt(3)), rs.getLong(4), rs.getLong(5),
                            null, integer(rs, 6), integer(rs, 7), scores.get(0), scores.get(1));
                },
                startYears(seasons));
        return distinctBy(rows, GameRow::gameId);
    }

    /** Points par période de chaque match, domicile puis extérieur. */
    private Map<String, List<List<Integer>>> periods() {
        int columns = 4 + OVERTIME_COLUMNS;
        List<Map.Entry<String, List<List<Integer>>>> rows = query("""
                SELECT %s, %s, %s FROM line_score
                WHERE substr(%s, 1, 3) IN ('002', '004')""".formatted(GAME_ID, periodColumns("home"), periodColumns("away"), GAME_ID),
                rs -> Map.entry(rs.getString(1),
                        List.of(periods(rs, 2, OVERTIME_COLUMNS), periods(rs, 2 + columns, OVERTIME_COLUMNS))));
        return rows.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));
    }

    private static String periodColumns(String side) {
        return Stream.concat(
                        Stream.of(1, 2, 3, 4).map(q -> "pts_qtr" + q + "_" + side),
                        Stream.iterate(1, ot -> ot + 1).limit(OVERTIME_COLUMNS).map(ot -> "pts_ot" + ot + "_" + side))
                .collect(Collectors.joining(", "));
    }

    @Override
    List<TeamGameRow> teamGames(List<String> seasons) {
        String where = "WHERE %s AND g.start_year IN (%s)".formatted(GAME_KINDS, placeholders(seasons));
        Object[] years = startYears(seasons);
        Object[] parameters = Stream.concat(Stream.of(years), Stream.of(years)).toArray();
        List<TeamGameRow> rows = query(GAMES + """
                SELECT g.gid, CAST(g.team_id_home AS INTEGER), %s FROM g %s
                UNION ALL
                SELECT g.gid, CAST(g.team_id_away AS INTEGER), %s FROM g %s"""
                        .formatted(boxScoreColumns("home"), where, boxScoreColumns("away"), where),
                rs -> new TeamGameRow(rs.getString(1), rs.getLong(2), integer(rs, 3), integer(rs, 4), integer(rs, 5),
                        integer(rs, 6), integer(rs, 7), integer(rs, 8), integer(rs, 9), integer(rs, 10), integer(rs, 11),
                        integer(rs, 12), integer(rs, 13), integer(rs, 14), integer(rs, 15), integer(rs, 16), integer(rs, 17)),
                parameters);
        return distinctBy(rows, row -> row.gameId() + "/" + row.teamId());
    }

    private static String boxScoreColumns(String side) {
        return Stream.of("pts", "fgm", "fga", "fg3m", "fg3a", "ftm", "fta", "oreb", "dreb", "reb", "ast", "stl", "blk",
                        "tov", "pf")
                .map(column -> "g." + column + "_" + side)
                .collect(Collectors.joining(", "));
    }

    /**
     * Classements recalculés : rang dans la conférence et dans la division au pourcentage de victoires
     * (puis au nombre de victoires), sur les matchs de saison régulière joués.
     */
    @Override
    List<StandingRow> standings(List<String> seasons) {
        Map<Long, String[]> alignment = teams().stream()
                .filter(team -> team.conference() != null)
                .collect(Collectors.toMap(TeamRow::teamId, team -> new String[]{team.conference(), team.division()},
                        (a, b) -> a));
        String regular = "substr(g.gid, 1, 3) = '002' AND g.start_year IN (%s) AND g.pts_home IS NOT NULL"
                .formatted(placeholders(seasons));
        Object[] years = startYears(seasons);
        Object[] parameters = Stream.concat(Stream.of(years), Stream.of(years)).toArray();
        List<SeasonRecord> records = query(GAMES + """
                SELECT start_year, team_id, SUM(won), COUNT(*) FROM (
                    SELECT g.start_year, CAST(g.team_id_home AS INTEGER) AS team_id,
                           CASE WHEN g.pts_home > g.pts_away THEN 1 ELSE 0 END AS won FROM g WHERE %s
                    UNION ALL
                    SELECT g.start_year, CAST(g.team_id_away AS INTEGER),
                           CASE WHEN g.pts_away > g.pts_home THEN 1 ELSE 0 END FROM g WHERE %s)
                GROUP BY start_year, team_id""".formatted(regular, regular),
                rs -> new SeasonRecord(season(rs.getInt(1)), rs.getLong(2), rs.getInt(3), rs.getInt(4)),
                parameters);

        List<StandingRow> standings = new ArrayList<>();
        records.stream()
                .filter(r -> alignment.containsKey(r.teamId()))
                .collect(Collectors.groupingBy(SeasonRecord::season))
                .forEach((season, teams) -> {
                    Map<Long, Integer> conferenceRanks = ranks(teams, r -> alignment.get(r.teamId())[0]);
                    Map<Long, Integer> divisionRanks = ranks(teams, r -> alignment.get(r.teamId())[1]);
                    teams.forEach(r -> standings.add(new StandingRow(season, r.teamId(),
                            conferenceRanks.get(r.teamId()), divisionRanks.get(r.teamId()))));
                });
        return standings;
    }

    private record SeasonRecord(String season, long teamId, int wins, int games) {
        double winPercentage() {
            return games == 0 ? 0 : (double) wins / games;
        }
    }

    private static Map<Long, Integer> ranks(List<SeasonRecord> teams, Function<SeasonRecord, String> group) {
        Map<Long, Integer> ranks = new HashMap<>();
        teams.stream().collect(Collectors.groupingBy(group)).values().forEach(members -> {
            List<SeasonRecord> sorted = members.stream()
                    .sorted(Comparator.comparingDouble(SeasonRecord::winPercentage).reversed()
                            .thenComparing(Comparator.comparingInt(SeasonRecord::wins).reversed())
                            .thenComparingLong(SeasonRecord::teamId))
                    .toList();
            for (int i = 0; i < sorted.size(); i++) {
                ranks.put(sorted.get(i).teamId(), i + 1);
            }
        });
        return ranks;
    }

    /** 2023 donne « 2023-24 », comme les saisons de nbadb. */
    static String season(int startYear) {
        return "%d-%02d".formatted(startYear, (startYear + 1) % 100);
    }

    private static Object[] startYears(List<String> seasons) {
        return seasons.stream().map(season -> Integer.parseInt(season.substring(0, 4))).toArray();
    }

    private static Map<String, String[]> alignment(String... values) {
        Map<String, String[]> alignment = new HashMap<>();
        for (int i = 0; i < values.length; i += 7) {
            for (int team = 2; team < 7; team++) {
                alignment.put(values[i + team], new String[]{values[i], values[i + 1]});
            }
        }
        return Map.copyOf(alignment);
    }
}
