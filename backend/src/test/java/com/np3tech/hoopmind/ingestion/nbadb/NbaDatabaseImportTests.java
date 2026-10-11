package com.np3tech.hoopmind.ingestion.nbadb;

import com.np3tech.hoopmind.IntegrationTest;
import com.np3tech.hoopmind.TestAccounts;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseImporter.ImportReport;
import com.np3tech.hoopmind.nba.repository.SeasonRepository;
import com.np3tech.hoopmind.nba.repository.TeamRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;

import static com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseFixture.BOS;
import static com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseFixture.LAL;
import static com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseFixture.NYK;
import static com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseFixture.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
class NbaDatabaseImportTests {

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

    @Autowired
    MockMvcTester mvc;

    @BeforeAll
    static void createDatabase() throws Exception {
        database = directory.resolve("nba.sqlite");
        try (NbaDatabaseFixture nba = new NbaDatabaseFixture(database)) {
            nba.played("0022200001", "2022-10-18", "2022-23", BOS, LAL, 120, 110);

            nba.played("0022300001", "2023-10-24T00:00:00", "2023-24", BOS, LAL, 110, 100);
            nba.played("0022300002", "2023-10-26", "2023-24", LAL, BOS, 105, 99);
            nba.played("0022300003", "2023-10-28", "2023-24", NYK, BOS, 120, 101);
            nba.played("0012300001", "2023-10-10", "2023-24", BOS, NYK, 90, 80);      // présaison : ignorée
            nba.played("0022300004", "2023-11-01", "2023-24", BOS, UNKNOWN, 100, 90); // équipe inconnue : ignorée
            nba.played("0042300101", "2024-04-20", "2023-24", BOS, NYK, 114, 94);
            nba.played("0042300401", "2024-06-06", "2023-24", BOS, LAL, 107, 89);
            nba.standing("2023-24", BOS, 1, 1);

            nba.played("0022400001", "2024-10-22", "2024-25", NYK, BOS, 109, 132);
            nba.scheduled("0022400002", "2025-01-15", "2024-25", BOS, LAL);
        }
    }

    private ImportReport importSeasons(int count) {
        NbaDatabaseImporter importer = new NbaDatabaseImporter(
                new NbaDatabaseImportProperties(database.toString(), count, false), teams, seasons, jdbc);
        return transaction.execute(status -> importer.importDatabase(null));
    }

    @Test
    void importsTheRequestedSeasonsAndComputesTeamRecords() {
        ImportReport report = importSeasons(2);

        assertThat(report.seasons()).containsExactly("2023-24", "2024-25");
        assertThat(report.teams()).isEqualTo(3);
        assertThat(report.games()).isEqualTo(7);
        assertThat(report.playedGames()).isEqualTo(6);
        assertThat(report.skippedGames()).isEqualTo(1);
        assertThat(report.teamGameStats()).isEqualTo(12);
        assertThat(report.teamSeasons()).isEqualTo(5);

        // L'équipe déjà présente (données initiales) est reliée à son identifiant NBA, pas dupliquée.
        assertThat(teams.findByAbbreviation("BOS")).get()
                .satisfies(team -> assertThat(team.getExternalId()).isEqualTo(BOS));

        assertThat(seasons.findByYear(2023)).get().satisfies(season -> {
            assertThat(season.getName()).isEqualTo("2023-2024");
            assertThat(season.getStartDate()).isEqualTo(LocalDate.of(2023, 10, 24));
            assertThat(season.getPlayoffsStartDate()).isEqualTo(LocalDate.of(2024, 4, 20));
            assertThat(season.getFinalsStartDate()).isEqualTo(LocalDate.of(2024, 6, 6));
            assertThat(season.getStatus().name()).isEqualTo("COMPLETED");
        });
        assertThat(seasons.findByYear(2024)).get()
                .satisfies(season -> assertThat(season.getStatus().name()).isEqualTo("IN_PROGRESS"));

        assertThat(jdbc.queryForObject("SELECT game_type FROM games WHERE external_id = '0042300401'", String.class))
                .isEqualTo("FINALS");
        assertThat(jdbc.queryForObject("SELECT status FROM games WHERE external_id = '0022400002'", String.class))
                .isEqualTo("SCHEDULED");
        assertThat(jdbc.queryForMap("""
                SELECT r.home_team_score, r.score_difference, w.abbreviation AS winner, r.home_team_quarter_scores_json AS quarters
                FROM game_results r JOIN games g ON g.id = r.game_id JOIN teams w ON w.id = r.winner_id
                WHERE g.external_id = '0022300002'"""))
                .containsEntry("home_team_score", 105)
                .containsEntry("score_difference", 6)
                .containsEntry("winner", "LAL")
                .containsEntry("quarters", "[26,26,26,27]");

        // Boston en saison régulière 2023-24 : une victoire à domicile puis deux défaites à l'extérieur.
        Map<String, Object> boston = jdbc.queryForMap("""
                SELECT ts.* FROM team_stats ts JOIN teams t ON t.id = ts.team_id JOIN seasons s ON s.id = ts.season_id
                WHERE t.abbreviation = 'BOS' AND s.year = 2023""");
        assertThat(boston)
                .containsEntry("wins", 1).containsEntry("losses", 2)
                .containsEntry("home_wins", 1).containsEntry("away_losses", 2)
                .containsEntry("conference_rank", 1)
                .containsEntry("current_streak", -2).containsEntry("longest_lose_streak", 2)
                .containsEntry("last_10_games_json", "[\"L\",\"L\",\"W\"]");
        assertThat((BigDecimal) boston.get("points_per_game")).isEqualByComparingTo("103.33");
        // 310 points en 3 × 98,8 possessions, 325 encaissés sur autant de possessions adverses
        assertThat((BigDecimal) boston.get("offensive_rating")).isEqualByComparingTo("104.59");
        assertThat((BigDecimal) boston.get("defensive_rating")).isEqualByComparingTo("109.65");
        assertThat((BigDecimal) boston.get("pace")).isEqualByComparingTo("98.80");
    }

    @Test
    void importCanBeReplayedWithoutDuplicates() {
        importSeasons(2);
        int games = countImportedGames();
        int stats = jdbc.queryForObject("SELECT count(*) FROM team_game_stats", Integer.class);

        importSeasons(2);

        assertThat(countImportedGames()).isEqualTo(games);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM team_game_stats", Integer.class)).isEqualTo(stats);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM teams WHERE abbreviation = 'BOS'", Integer.class)).isEqualTo(1);
    }

    @Test
    void seasonCountLimitsTheImport() {
        assertThat(importSeasons(3).seasons()).containsExactly("2022-23", "2023-24", "2024-25");
        assertThat(importSeasons(1).seasons()).containsExactly("2024-25");
    }

    @Test
    void missingDatabaseFileIsReportedToTheAdministrator() {
        NbaDatabaseImporter importer = new NbaDatabaseImporter(
                new NbaDatabaseImportProperties(directory.resolve("absent.sqlite").toString(), 2, false), teams, seasons, jdbc);

        assertThatThrownBy(() -> importer.importDatabase(null))
                .isInstanceOf(NbaImportException.class)
                .hasMessageContaining("download-nba-database.sh");
    }

    @Test
    void onlyAdministratorsCanStartAnImport() {
        assertThat(mvc.post().uri("/api/v1/admin/imports/nba-database")
                .header(HttpHeaders.AUTHORIZATION, TestAccounts.register(mvc).bearer()))
                .hasStatus(403);

        // Le fichier par défaut (data/nba.sqlite) n'existe pas pendant les tests : erreur explicite, pas d'erreur 500.
        assertThat(mvc.post().uri("/api/v1/admin/imports/nba-database").param("seasons", "2")
                .header(HttpHeaders.AUTHORIZATION, TestAccounts.admin(mvc).bearer()))
                .hasStatus(422)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void theDataPageSeesTheFileAndTheKaggleKeyBeforeAnyBackgroundImport() {
        String admin = TestAccounts.admin(mvc).bearer();
        assertThat(mvc.get().uri("/api/v1/admin/imports/nba-database/status").header(HttpHeaders.AUTHORIZATION, admin))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.file.present").isEqualTo(false);
                    assertThat(json).extractingPath("$.kaggleConfigured").isEqualTo(false);
                    assertThat(json).extractingPath("$.kaggleDataset").isEqualTo("wyattowalsh/basketball");
                });

        // Sans fichier ni clé Kaggle, l'import en arrière-plan est refusé avec une explication.
        assertThat(mvc.post().uri("/api/v1/admin/imports/nba-database/jobs").header(HttpHeaders.AUTHORIZATION, admin))
                .hasStatus(422);
        assertThat(mvc.post().uri("/api/v1/admin/imports/nba-database/jobs").param("download", "true")
                .header(HttpHeaders.AUTHORIZATION, admin))
                .hasStatus(422)
                .bodyJson().extractingPath("$.detail").asString().contains("KAGGLE_KEY");
    }

    private int countImportedGames() {
        return jdbc.queryForObject("SELECT count(*) FROM games WHERE external_id IS NOT NULL", Integer.class);
    }
}
