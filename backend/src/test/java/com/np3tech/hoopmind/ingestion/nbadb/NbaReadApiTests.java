package com.np3tech.hoopmind.ingestion.nbadb;

import com.np3tech.hoopmind.IntegrationTest;
import com.np3tech.hoopmind.TestAccounts;
import com.np3tech.hoopmind.nba.repository.SeasonRepository;
import com.np3tech.hoopmind.nba.repository.TeamRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Path;
import java.time.Clock;

import static com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseFixture.BOS;
import static com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseFixture.LAL;
import static com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseFixture.NYK;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les lectures utilisées par l'espace connecté (matchs d'une saison, classement) après un import réel. Elles
 * échouaient en 500 : la réponse était construite hors session JPA, sur des équipes et une saison non chargées.
 */
@IntegrationTest
class NbaReadApiTests {

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

    private long seasonId;
    private String bearer;

    @BeforeAll
    static void createDatabase() throws Exception {
        database = directory.resolve("nba.sqlite");
        try (NbaDatabaseFixture nba = new NbaDatabaseFixture(database)) {
            nba.played("0022400001", "2024-10-22", "2024-25", BOS, LAL, 110, 100);
            nba.played("0022400002", "2024-10-24", "2024-25", NYK, BOS, 101, 98);
            nba.scheduled("0022400003", "2025-01-15", "2024-25", LAL, NYK);
            nba.standing("2024-25", BOS, 1, 1);
        }
    }

    @BeforeEach
    void importSeason() {
        NbaDatabaseImporter importer = new NbaDatabaseImporter(
                new NbaDatabaseImportProperties(database.toString(), 1, false), teams, seasons, jdbc, Clock.systemUTC());
        transaction.execute(status -> importer.importDatabase(null));
        seasonId = seasons.findByYear(2024).orElseThrow().getId();
        bearer = TestAccounts.register(mvc).bearer();
    }

    // La base est partagée par les tests d'intégration : on ne regarde que les matchs de ce jeu de données.
    @Test
    void theGamesOfASeasonComeWithTeamsAndScores() {
        int bostonHome = gameId("0022400001");
        int newYorkHome = gameId("0022400002");
        int scheduled = gameId("0022400003");

        assertThat(mvc.get().uri("/api/v1/games/season/{id}", seasonId).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$[*].id").asArray()
                            .contains(bostonHome, newYorkHome, scheduled);
                    assertThat(json).extractingPath("$[?(@.id == " + bostonHome + ")].result.homeTeamScore")
                            .asArray().containsExactly(110);
                    assertThat(json).extractingPath("$[?(@.id == " + bostonHome + ")].homeTeam.abbreviation")
                            .asArray().containsExactly("BOS");
                    assertThat(json).extractingPath("$[?(@.id == " + newYorkHome + ")].result.winner.abbreviation")
                            .asArray().containsExactly("NYK");
                    // Les champs nuls ne sont pas sérialisés : un match à venir n'a pas de résultat.
                    assertThat(json).extractingPath("$[?(@.id == " + scheduled + ")].result").asArray().isEmpty();
                    assertThat(json).extractingPath("$[?(@.id == " + bostonHome + ")].season.year")
                            .asArray().containsExactly(2024);
                });
    }

    @Test
    void oneGameCanBeReadWithItsScore() {
        int bostonHome = gameId("0022400001");

        assertThat(mvc.get().uri("/api/v1/games/{id}", bostonHome).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatusOk()
                .bodyJson().extractingPath("$.result.awayTeamScore").isEqualTo(100);
    }

    /** Identifiant en int : c'est ainsi que le JSON des réponses le relit. */
    private int gameId(String externalId) {
        return jdbc.queryForObject("SELECT id FROM games WHERE external_id = ?", Integer.class, externalId);
    }

    @Test
    void theStandingsOfASeasonNameTheirTeams() {
        assertThat(mvc.get().uri("/api/v1/stats/standings/season/{id}", seasonId).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$[?(@.team.abbreviation == 'BOS')].wins").asArray().containsExactly(1);
                    assertThat(json).extractingPath("$[?(@.team.abbreviation == 'BOS')].season.year").asArray().containsExactly(2024);
                });
    }
}
