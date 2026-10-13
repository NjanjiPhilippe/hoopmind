package com.np3tech.hoopmind.ingestion.nbadb;

import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.GameRow;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.StandingRow;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.TeamGameRow;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.TeamRow;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.np3tech.hoopmind.ingestion.nbadb.StatsNbaFixture.BOS;
import static com.np3tech.hoopmind.ingestion.nbadb.StatsNbaFixture.LAL;
import static com.np3tech.hoopmind.ingestion.nbadb.StatsNbaFixture.NYK;
import static com.np3tech.hoopmind.ingestion.nbadb.StatsNbaFixture.POR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatsNbaSourceTests {

    private static StatsNbaSource source() {
        List<StatsNbaClient.GameLogLine> season = new ArrayList<>(new StatsNbaFixture()
                .played("0022400001", "2024-10-22", BOS, NYK, 132, 109)
                .played("0022400002", "2024-10-24", NYK, POR, 110, 105)
                .played("0022400003", "2024-10-26", LAL, POR, 120, 101)
                .orphan("0022400004", "2024-10-27", BOS, LAL, 100)
                .lines());
        season.addAll(new StatsNbaFixture().played("0042400401", "2025-06-05", LAL, BOS, 111, 104).lines());
        List<StatsNbaClient.GameLogLine> playoffsOnly = new StatsNbaFixture()
                .played("0042500101", "2026-04-18", BOS, NYK, 100, 90).lines();
        return new StatsNbaSource(Map.of("2024-25", season, "2025-26", playoffsOnly));
    }

    @Test
    void bothLinesOfAGameBecomeOneGameWithTheHomeTeamFromTheMatchup() {
        Map<String, GameRow> games = source().games(List.of("2024-25")).stream()
                .collect(Collectors.toMap(GameRow::gameId, g -> g));

        assertThat(games).containsOnlyKeys("0022400001", "0022400002", "0022400003", "0042400401");
        assertThat(games.get("0022400001")).isEqualTo(new GameRow("0022400001", LocalDate.of(2024, 10, 22), "2024-25",
                BOS, NYK, null, 132, 109, List.of(), List.of()));
        assertThat(games.get("0042400401").finals()).isTrue();
        assertThat(games.get("0042400401").homeTeamId()).isEqualTo(LAL);
    }

    @Test
    void eachTeamKeepsItsBoxScore() {
        List<TeamGameRow> lines = source().teamGames(List.of("2024-25"));

        assertThat(lines).hasSize(8);
        assertThat(lines).filteredOn(l -> l.gameId().equals("0022400002") && l.teamId() == POR).singleElement()
                .satisfies(line -> {
                    assertThat(line.points()).isEqualTo(105);
                    assertThat(line.fga()).isEqualTo(88);
                    assertThat(line.tov()).isEqualTo(12);
                });
    }

    @Test
    void teamsGetTheirCityAndTheCurrentAlignment() {
        assertThat(source().teams()).contains(
                new TeamRow(POR, "POR", "Portland Trail Blazers", "Portland", null, null, "West", "Northwest"),
                new TeamRow(BOS, "BOS", "Boston Celtics", "Boston", null, null, "East", "Atlantic"));
        assertThat(StatsNbaSource.city("Los Angeles Lakers")).isEqualTo("Los Angeles");
        assertThat(StatsNbaSource.city("LA Clippers")).isEqualTo("LA");
    }

    @Test
    void onlySeasonsWithRegularSeasonGamesAreOffered() {
        assertThat(source().recentSeasons(5)).containsExactly("2024-25");
    }

    @Test
    void standingsAreRecomputedFromRegularSeasonResults() {
        Map<Long, StandingRow> standings = source().standings(List.of("2024-25")).stream()
                .collect(Collectors.toMap(StandingRow::teamId, s -> s));

        // Est : BOS 1-0, NYK 1-1. Ouest : LAL 1-0, POR 0-2 (la finale ne compte pas).
        assertThat(standings.get(BOS)).isEqualTo(new StandingRow("2024-25", BOS, 1, 1));
        assertThat(standings.get(NYK)).isEqualTo(new StandingRow("2024-25", NYK, 2, 2));
        assertThat(standings.get(LAL)).isEqualTo(new StandingRow("2024-25", LAL, 1, 1));
        assertThat(standings.get(POR)).isEqualTo(new StandingRow("2024-25", POR, 2, 1));
    }

    @Test
    void datesAreReadInBothFormatsOfTheApi() {
        assertThat(StatsNbaSource.date("2024-10-22")).isEqualTo(LocalDate.of(2024, 10, 22));
        assertThat(StatsNbaSource.date("2024-10-22T00:00:00")).isEqualTo(LocalDate.of(2024, 10, 22));
        assertThat(StatsNbaSource.date("OCT 22, 2024")).isEqualTo(LocalDate.of(2024, 10, 22));
        assertThatThrownBy(() -> StatsNbaSource.date("hier")).isInstanceOf(NbaImportException.class);
    }
}
