package com.np3tech.hoopmind.ingestion.nbadb;

import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.GameRow;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.StandingRow;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.TeamGameRow;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseRows.TeamRow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.np3tech.hoopmind.ingestion.nbadb.LegacyKaggleFixture.BOS;
import static com.np3tech.hoopmind.ingestion.nbadb.LegacyKaggleFixture.LAL;
import static com.np3tech.hoopmind.ingestion.nbadb.LegacyKaggleFixture.NYK;
import static com.np3tech.hoopmind.ingestion.nbadb.LegacyKaggleFixture.PHI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LegacyKaggleReaderTests {

    @TempDir
    static Path directory;

    static Path database;

    @BeforeAll
    static void createDatabase() throws Exception {
        database = directory.resolve("legacy.sqlite");
        try (LegacyKaggleFixture nba = new LegacyKaggleFixture(database)) {
            nba.played("0021800001", "22018", "2018-10-16", BOS, PHI, 105, 87, "Regular Season");

            nba.played("0021900001", "22019", "2019-10-22", BOS, LAL, 110, 100, "Regular Season");
            // Identifiant stocké en nombre, sans ses zéros : il doit redevenir « 0021900002 ».
            nba.played(21900002L, "22019", "2019-10-24", NYK, BOS, 120, 118, "Regular Season");
            nba.played("0021900003", "22019", "2019-10-26", PHI, NYK, 99, 101, "Regular Season");
            nba.played("0011900001", "12019", "2019-10-05", BOS, NYK, 90, 80, "Pre Season");
            nba.played("0041900401", "42019", "2020-09-30", LAL, BOS, 116, 98, "Playoffs");
            nba.lineScore("0021900001", new int[]{30, 25, 25, 30}, new int[]{20, 30, 25, 25});
            nba.lineScore(21900002L, new int[]{28, 27, 25, 28, 7, 5}, new int[]{25, 30, 25, 28, 7, 3});
        }
    }

    @Test
    void theOldKaggleFormatIsRecognized() {
        try (NbaDatabaseReader reader = NbaDatabaseReader.open(database)) {
            assertThat(reader).isInstanceOf(LegacyKaggleReader.class);
        }
    }

    @Test
    void seasonsComeFromSeasonIdInTheNbadbFormat() {
        try (NbaDatabaseReader reader = NbaDatabaseReader.open(database)) {
            assertThat(reader.recentSeasons(5)).containsExactly("2018-19", "2019-20");
            assertThat(reader.recentSeasons(1)).containsExactly("2019-20");
        }
        assertThat(LegacyKaggleReader.season(1999)).isEqualTo("1999-00");
    }

    @Test
    void teamsGetTheirArenaAndTheCurrentAlignment() {
        try (NbaDatabaseReader reader = NbaDatabaseReader.open(database)) {
            assertThat(reader.teams()).contains(
                    new TeamRow(BOS, "BOS", "Boston Celtics", "Boston", "TD Garden", 1946, "East", "Atlantic"),
                    new TeamRow(LAL, "LAL", "Los Angeles Lakers", "Los Angeles", "Crypto.com Arena", 1946, "West", "Pacific"));
        }
    }

    @Test
    void gamesKeepRegularSeasonAndPlayoffsWithNormalizedIdsAndPeriods() {
        try (NbaDatabaseReader reader = NbaDatabaseReader.open(database)) {
            Map<String, GameRow> games = reader.games(List.of("2019-20")).stream()
                    .collect(Collectors.toMap(GameRow::gameId, g -> g));

            assertThat(games).containsOnlyKeys("0021900001", "0021900002", "0021900003", "0041900401");
            assertThat(games.get("0021900001")).satisfies(game -> {
                assertThat(game.gameDate()).isEqualTo(LocalDate.of(2019, 10, 22));
                assertThat(game.seasonYear()).isEqualTo("2019-20");
                assertThat(game.homeTeamId()).isEqualTo(BOS);
                assertThat(game.homePoints()).isEqualTo(110);
                assertThat(game.homePeriods()).containsExactly(30, 25, 25, 30);
                assertThat(game.played()).isTrue();
            });
            assertThat(games.get("0021900002").overtimePeriods()).isEqualTo(2);
            assertThat(games.get("0021900003").homePeriods()).as("sans line_score, pas de périodes").isEmpty();
            assertThat(games.get("0041900401").finals()).isTrue();
        }
    }

    @Test
    void eachGameGivesOneBoxScoreLinePerTeam() {
        try (NbaDatabaseReader reader = NbaDatabaseReader.open(database)) {
            List<TeamGameRow> lines = reader.teamGames(List.of("2019-20"));

            assertThat(lines).hasSize(8);
            assertThat(lines).filteredOn(line -> line.gameId().equals("0021900002") && line.teamId() == BOS)
                    .singleElement()
                    .satisfies(line -> {
                        assertThat(line.points()).isEqualTo(118);
                        assertThat(line.fga()).isEqualTo(88);
                        assertThat(line.tov()).isEqualTo(12);
                    });
        }
    }

    @Test
    void standingsAreRecomputedFromRegularSeasonResults() {
        try (NbaDatabaseReader reader = NbaDatabaseReader.open(database)) {
            Map<Long, StandingRow> standings = reader.standings(List.of("2019-20")).stream()
                    .collect(Collectors.toMap(StandingRow::teamId, s -> s));

            // Est : NYK 2-0, BOS 1-1, PHI 0-1 (la finale perdue par Boston ne compte pas). Ouest : LAL seul.
            assertThat(standings.get(NYK)).isEqualTo(new StandingRow("2019-20", NYK, 1, 1));
            assertThat(standings.get(BOS)).isEqualTo(new StandingRow("2019-20", BOS, 2, 2));
            assertThat(standings.get(PHI)).isEqualTo(new StandingRow("2019-20", PHI, 3, 3));
            assertThat(standings.get(LAL)).isEqualTo(new StandingRow("2019-20", LAL, 1, 1));
        }
    }

    @Test
    void anUnknownFileFormatIsReportedClearly() throws Exception {
        Path other = directory.resolve("other.sqlite");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + other.toAbsolutePath());
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE something (id INTEGER)");
        }

        assertThatThrownBy(() -> NbaDatabaseReader.open(other))
                .isInstanceOf(NbaImportException.class)
                .hasMessageContaining("Format de base NBA inconnu");
    }
}
