package com.np3tech.hoopmind.ingestion.nbadb;

import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.GameRow;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.StandingRow;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.TeamRow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Pour les sources qui ne donnent ni conférence, ni division, ni classement (ancien format Kaggle, stats.nba.com) :
 * l'alignement actuel de la NBA, et des classements recalculés à partir des résultats de saison régulière.
 */
final class NbaAlignment {

    /** Alignement de la NBA depuis 2004-05 : abréviation, conférence, division. */
    private static final Map<String, Alignment> ALIGNMENT = alignment(
            "East", "Atlantic", "BOS", "BKN", "NYK", "PHI", "TOR",
            "East", "Central", "CHI", "CLE", "DET", "IND", "MIL",
            "East", "Southeast", "ATL", "CHA", "MIA", "ORL", "WAS",
            "West", "Northwest", "DEN", "MIN", "OKC", "POR", "UTA",
            "West", "Pacific", "GSW", "LAC", "LAL", "PHX", "SAC",
            "West", "Southwest", "DAL", "HOU", "MEM", "NOP", "SAS");

    record Alignment(String conference, String division) {
    }

    private NbaAlignment() {
    }

    static Optional<Alignment> of(String abbreviation) {
        return Optional.ofNullable(abbreviation == null ? null : ALIGNMENT.get(abbreviation));
    }

    /**
     * Classements recalculés : rang dans la conférence et dans la division au pourcentage de victoires
     * (puis au nombre de victoires, puis à l'identifiant), sur les matchs de saison régulière joués.
     */
    static List<StandingRow> standings(List<GameRow> games, List<TeamRow> teams) {
        Map<Long, Alignment> alignment = teams.stream()
                .filter(team -> team.conference() != null && team.division() != null)
                .collect(Collectors.toMap(TeamRow::teamId, team -> new Alignment(team.conference(), team.division()),
                        (a, b) -> a));
        Map<String, Map<Long, int[]>> records = new HashMap<>();
        games.stream().filter(g -> g.played() && !g.playoffs()).forEach(g -> {
            Map<Long, int[]> season = records.computeIfAbsent(g.seasonYear(), s -> new HashMap<>());
            boolean homeWon = g.homePoints() > g.awayPoints();
            count(season, g.homeTeamId(), homeWon);
            count(season, g.awayTeamId(), !homeWon);
        });

        List<StandingRow> standings = new ArrayList<>();
        records.forEach((season, byTeam) -> {
            List<SeasonRecord> teamRecords = byTeam.entrySet().stream()
                    .filter(e -> alignment.containsKey(e.getKey()))
                    .map(e -> new SeasonRecord(e.getKey(), e.getValue()[0], e.getValue()[1]))
                    .toList();
            Map<Long, Integer> conferenceRanks = ranks(teamRecords, r -> alignment.get(r.teamId()).conference());
            Map<Long, Integer> divisionRanks = ranks(teamRecords, r -> alignment.get(r.teamId()).division());
            teamRecords.forEach(r -> standings.add(new StandingRow(season, r.teamId(),
                    conferenceRanks.get(r.teamId()), divisionRanks.get(r.teamId()))));
        });
        return standings;
    }

    private static void count(Map<Long, int[]> season, long teamId, boolean won) {
        int[] record = season.computeIfAbsent(teamId, id -> new int[2]);
        record[0] += won ? 1 : 0;
        record[1]++;
    }

    private record SeasonRecord(long teamId, int wins, int games) {
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

    private static Map<String, Alignment> alignment(String... values) {
        Map<String, Alignment> alignment = new HashMap<>();
        for (int i = 0; i < values.length; i += 7) {
            for (int team = 2; team < 7; team++) {
                alignment.put(values[i + team], new Alignment(values[i], values[i + 1]));
            }
        }
        return Map.copyOf(alignment);
    }
}
