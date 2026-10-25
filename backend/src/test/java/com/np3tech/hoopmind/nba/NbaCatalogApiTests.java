package com.np3tech.hoopmind.nba;

import com.jayway.jsonpath.JsonPath;
import com.np3tech.hoopmind.IntegrationTest;
import com.np3tech.hoopmind.TestAccounts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Écriture et lecture du catalogue NBA par l'API : les services reçoivent les requêtes (plus d'entité construite par
 * le contrôleur), et les listes publiques sont paginées.
 */
@IntegrationTest
class NbaCatalogApiTests {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcTemplate jdbc;

    private String admin;
    private long seasonId;
    private long homeTeamId;
    private long awayTeamId;

    @BeforeEach
    void createCatalog() throws Exception {
        admin = TestAccounts.admin(mvc).bearer();
        // La base est partagée par les tests d'intégration : année et abréviations tirées au hasard.
        int year = ThreadLocalRandom.current().nextInt(3000, 9000);
        seasonId = create("/api/v1/seasons", """
                {"year":%d,"name":"%d-%d","startDate":"%d-10-20","endDate":"%d-04-15","status":"IN_PROGRESS"}
                """.formatted(year, year, (year + 1) % 100, year, year + 1));
        homeTeamId = create("/api/v1/teams", team("Home"));
        awayTeamId = create("/api/v1/teams", team("Away"));
    }

    /** D'autres tests comptent les 30 équipes de référence : on retire tout ce que ce test a créé. */
    @AfterEach
    void deleteCatalog() {
        jdbc.update("DELETE FROM games WHERE season_id = ?", seasonId);
        jdbc.update("DELETE FROM players WHERE team_id IN (?, ?)", homeTeamId, awayTeamId);
        jdbc.update("DELETE FROM teams WHERE id IN (?, ?)", homeTeamId, awayTeamId);
        jdbc.update("DELETE FROM seasons WHERE id = ?", seasonId);
    }

    @Test
    void playersAreListedWithTheirTeamOnePageAtATime() throws Exception {
        for (int i = 0; i < 3; i++) {
            create("/api/v1/players", """
                    {"firstName":"Joueur","lastName":"Test%d","position":"CENTER","teamId":%d}
                    """.formatted(i, homeTeamId));
        }

        MvcTestResult page = mvc.get().uri("/api/v1/players/position/CENTER?size=2").exchange();

        assertThat(page).hasStatusOk();
        assertThat(page).bodyJson().extractingPath("$.content.length()").isEqualTo(2);
        assertThat(page).bodyJson().extractingPath("$.content[0].team.id").isNotNull();
        assertThat(page).bodyJson().extractingPath("$.page.size").isEqualTo(2);
    }

    @Test
    void aPageHoldsAtMostOneHundredItems() {
        assertThat(mvc.get().uri("/api/v1/games?size=5000").exchange())
                .hasStatusOk()
                .bodyJson().extractingPath("$.page.size").isEqualTo(100);
    }

    @Test
    void aTeamCannotPlayItself() {
        assertThat(mvc.post().uri("/api/v1/games").header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON).content(game(homeTeamId, homeTeamId)))
                .hasStatus(400)
                .bodyJson().extractingPath("$.code").isEqualTo("INVALID_GAME_DATA");
    }

    @Test
    void updatingAGameKeepsWhatIsNotSentAndAppliesEveryField() throws Exception {
        long gameId = create("/api/v1/games", game(homeTeamId, awayTeamId));

        MvcTestResult updated = mvc.put().uri("/api/v1/games/{id}", gameId).header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"broadcastNetwork":"ESPN","isNationalTv":true}
                        """).exchange();

        assertThat(updated).hasStatusOk();
        assertThat(updated).bodyJson().extractingPath("$.broadcastNetwork").isEqualTo("ESPN");
        assertThat(updated).bodyJson().extractingPath("$.isNationalTv").isEqualTo(true);
        assertThat(updated).bodyJson().extractingPath("$.venue").isEqualTo("Salle de test");
        assertThat(updated).bodyJson().extractingPath("$.homeTeam.id").isEqualTo((int) homeTeamId);
    }

    @Test
    void theDateRangeIsLimitedToThirtyOneDays() {
        assertThat(mvc.get().uri("/api/v1/games/date-range?startDate=2025-01-01T00:00:00&endDate=2025-03-01T00:00:00")
                .exchange())
                .hasStatus(400)
                .bodyJson().extractingPath("$.code").isEqualTo("INVALID_DATE_RANGE");
        assertThat(mvc.get().uri("/api/v1/games/date-range?startDate=2025-01-01T00:00:00&endDate=2025-01-31T00:00:00")
                .exchange())
                .hasStatusOk();
    }

    private String team(String name) {
        String abbreviation = "Q" + Long.toString(ThreadLocalRandom.current().nextLong(1L << 30), 36).toUpperCase();
        return """
                {"name":"%s","city":"Testville","abbreviation":"%s","conference":"EASTERN","division":"ATLANTIC"}
                """.formatted(name, abbreviation);
    }

    private String game(long home, long away) {
        return """
                {"homeTeamId":%d,"awayTeamId":%d,"seasonId":%d,"gameType":"REGULAR_SEASON",
                 "scheduledDate":"2025-01-10T19:30:00","venue":"Salle de test"}
                """.formatted(home, away, seasonId);
    }

    private long create(String uri, String body) throws Exception {
        MvcTestResult result = mvc.post().uri(uri).header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON).content(body).exchange();
        assertThat(result).as("POST %s", uri).hasStatus(201);
        return JsonPath.<Number>read(result.getResponse().getContentAsString(), "$.id").longValue();
    }
}
