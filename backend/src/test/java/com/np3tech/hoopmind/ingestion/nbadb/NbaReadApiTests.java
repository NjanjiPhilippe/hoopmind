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

    @Test
    void theGamesOfASeasonComeWithTeamsAndScores() {
        assertThat(mvc.get().uri("/api/v1/games/season/{id}", seasonId).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.length()").isEqualTo(3);
                    assertThat(json).extractingPath("$[?(@.homeTeam.abbreviation == 'BOS')].result.homeTeamScore")
                            .asArray().containsExactly(110);
                    assertThat(json).extractingPath("$[?(@.homeTeam.abbreviation == 'NYK')].result.winner.abbreviation")
                            .asArray().containsExactly("NYK");
                    // Les champs nuls ne sont pas sérialisés : un match à venir n'a pas de résultat.
                    assertThat(json).extractingPath("$[?(@.status == 'SCHEDULED')].result").asArray().isEmpty();
                    assertThat(json).extractingPath("$[0].season.year").isEqualTo(2024);
                });
    }

    @Test
    void oneGameAndTheGamesOfATeamCanBeRead() {
        long gameId = jdbc.queryForObject("SELECT id FROM games WHERE external_id = '0022400001'", Long.class);
        long bostonId = teams.findByAbbreviation("BOS").orElseThrow().getId();

        assertThat(mvc.get().uri("/api/v1/games/{id}", gameId).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatusOk()
                .bodyJson().extractingPath("$.result.awayTeamScore").isEqualTo(100);
        assertThat(mvc.get().uri("/api/v1/games/team/{id}", bostonId).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatusOk()
                .bodyJson().extractingPath("$.length()").isEqualTo(2);
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
