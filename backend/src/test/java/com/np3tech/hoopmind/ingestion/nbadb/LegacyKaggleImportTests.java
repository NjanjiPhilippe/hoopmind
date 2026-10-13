package com.np3tech.hoopmind.ingestion.nbadb;

import com.np3tech.hoopmind.IntegrationTest;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseImporter.ImportReport;
import com.np3tech.hoopmind.nba.repository.SeasonRepository;
import com.np3tech.hoopmind.nba.repository.TeamRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Path;
import java.time.Clock;
import java.util.Map;

import static com.np3tech.hoopmind.ingestion.nbadb.LegacyKaggleFixture.BOS;
import static com.np3tech.hoopmind.ingestion.nbadb.LegacyKaggleFixture.LAL;
import static com.np3tech.hoopmind.ingestion.nbadb.LegacyKaggleFixture.NYK;
import static com.np3tech.hoopmind.ingestion.nbadb.LegacyKaggleFixture.PHI;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * L'archive Kaggle livre encore l'ancien format : l'import doit en tirer les mêmes données que du schéma nbadb.
 * Les saisons (2016-17, 2017-18) ne croisent pas celles de {@link NbaDatabaseImportTests}, qui partage la base.
 */
@IntegrationTest
class LegacyKaggleImportTests {

    @TempDir
    static Path directory;

    static Path database;

    @Autowired
    TeamRepository teams;

    @Autowired
    SeasonRepository seasons;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TransactionTemplate transaction;

    @BeforeAll
    static void createDatabase() throws Exception {
        database = directory.resolve("nba.sqlite");
        try (LegacyKaggleFixture nba = new LegacyKaggleFixture(database)) {
            nba.played("0021600001", "22016", "2016-10-25", BOS, LAL, 100, 90, "Regular Season");
            nba.played("0021700001", "22017", "2017-10-17", BOS, LAL, 110, 100, "Regular Season");
            nba.played(21700002L, "22017", "2017-10-19", NYK, BOS, 105, 99, "Regular Season");
            nba.played("0021700003", "22017", "2017-10-21", PHI, NYK, 120, 101, "Regular Season");
            nba.played("0041700401", "42017", "2018-06-01", BOS, LAL, 107, 89, "Playoffs");
            nba.lineScore(21700002L, new int[]{26, 26, 26, 27}, new int[]{25, 25, 25, 24});
        }
    }

    private ImportReport importSeasons(int count) {
        NbaDatabaseImporter importer = new NbaDatabaseImporter(
                new NbaDatabaseImportProperties(database.toString(), count, false), teams, seasons, jdbc, Clock.systemUTC());
        return transaction.execute(status -> importer.importDatabase(null));
    }

    @Test
    void theOldKaggleFormatIsImportedLikeNbadb() {
        ImportReport report = importSeasons(1);

        assertThat(report.seasons()).containsExactly("2017-18");
        assertThat(report.teams()).isEqualTo(4);
        assertThat(report.games()).isEqualTo(4);
        assertThat(report.playedGames()).isEqualTo(4);
        assertThat(report.skippedGames()).isZero();
        assertThat(report.teamGameStats()).isEqualTo(8);
        assertThat(report.teamSeasons()).isEqualTo(4);

        assertThat(seasons.findByYear(2017)).get().satisfies(season -> {
            assertThat(season.getName()).isEqualTo("2017-2018");
            assertThat(season.getFinalsStartDate()).hasToString("2018-06-01");
        });
        assertThat(jdbc.queryForMap("""
                SELECT r.home_team_score, w.abbreviation AS winner, r.home_team_quarter_scores_json AS quarters
                FROM game_results r JOIN games g ON g.id = r.game_id JOIN teams w ON w.id = r.winner_id
                WHERE g.external_id = '0021700002'"""))
                .containsEntry("home_team_score", 105)
                .containsEntry("winner", "NYK")
                .containsEntry("quarters", "[26,26,26,27]");

        // Classement recalculé : PHI 1-0, BOS 1-1, NYK 1-1 (NYK perd contre PHI), départagés au nombre de victoires puis à l'identifiant.
        Map<String, Object> philadelphia = jdbc.queryForMap("""
                SELECT ts.wins, ts.losses, ts.conference_rank, ts.division_rank FROM team_stats ts
                JOIN teams t ON t.id = ts.team_id JOIN seasons s ON s.id = ts.season_id
                WHERE t.abbreviation = 'PHI' AND s.year = 2017""");
        assertThat(philadelphia)
                .containsEntry("wins", 1).containsEntry("losses", 0)
                .containsEntry("conference_rank", 1).containsEntry("division_rank", 1);
        assertThat(teams.findByAbbreviation("PHI")).get()
                .satisfies(team -> assertThat(team.getArena()).isEqualTo("Wells Fargo Center"));
    }
}
