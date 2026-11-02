package com.np3tech.hoopmind.ml.history;

import com.jayway.jsonpath.JsonPath;
import com.np3tech.hoopmind.IntegrationTest;
import com.np3tech.hoopmind.TestAccounts;
import com.np3tech.hoopmind.TestAccounts.Session;
import com.np3tech.hoopmind.ml.scoring.GameOutcomeFeatures;
import com.np3tech.hoopmind.ml.scoring.ScoringCard;
import com.np3tech.hoopmind.ml.scoring.ScoringCardStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La page Prédire enregistre chaque prédiction expliquée dans « Mes prédictions » de celui qui la demande, et de
 * personne d'autre. Une carte de modèle enregistrée avec une autre liste de variables n'est jamais appliquée.
 */
@IntegrationTest
class ExplainedPredictionHistoryTests {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ScoringCardStore cards;

    private String admin;
    private long seasonId;
    private long homeTeamId;
    private long awayTeamId;
    private LocalDate gameDay;

    @BeforeEach
    void createPlayedGame() throws Exception {
        admin = TestAccounts.admin(mvc).bearer();
        int year = ThreadLocalRandom.current().nextInt(3000, 9000);
        gameDay = LocalDate.of(year, 12, 1);
        seasonId = create("/api/v1/seasons", """
                {"year":%d,"name":"%d-%d","startDate":"%d-10-20","endDate":"%d-04-15","status":"IN_PROGRESS"}
                """.formatted(year, year, (year + 1) % 100, year, year + 1));
        homeTeamId = create("/api/v1/teams", team("Home"));
        awayTeamId = create("/api/v1/teams", team("Away"));
        long gameId = create("/api/v1/games", """
                {"homeTeamId":%d,"awayTeamId":%d,"seasonId":%d,"gameType":"REGULAR_SEASON",
                 "scheduledDate":"%sT19:30:00","venue":"Salle de test"}
                """.formatted(homeTeamId, awayTeamId, seasonId, gameDay.minusDays(10)));
        assertThat(mvc.put().uri("/api/v1/games/{id}/result", gameId).header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"homeTeamScore":110,"awayTeamScore":104}
                        """)).hasStatusOk();
    }

    @AfterEach
    void cleanUp() {
        // Les autres tests attendent un modèle non entraîné : une carte aux variables périmées est ignorée.
        cards.save(obsoleteCard());
        jdbc.update("DELETE FROM prediction_history WHERE season_id = ?", seasonId);
        jdbc.update("DELETE FROM game_results WHERE game_id IN (SELECT id FROM games WHERE season_id = ?)", seasonId);
        jdbc.update("DELETE FROM games WHERE season_id = ?", seasonId);
        jdbc.update("DELETE FROM teams WHERE id IN (?, ?)", homeTeamId, awayTeamId);
        jdbc.update("DELETE FROM seasons WHERE id = ?", seasonId);
    }

    @Test
    void anExplainedPredictionIsSavedForTheUserWhoAskedOnly() {
        cards.save(new ScoringCard("logistic-regression", "v-test", GameOutcomeFeatures.NAMES,
                new double[]{0, 0, 0, 0, 0}, new double[]{1, 1, 1, 1, 1}, new double[]{1, 0.5, 0.05, 0.1, 0}, 0.1));
        Session alice = TestAccounts.register(mvc);
        Session bob = TestAccounts.register(mvc);

        assertThat(explain(alice)).hasStatusOk();

        assertThat(mvc.get().uri("/api/v1/predictions/mine").header(HttpHeaders.AUTHORIZATION, alice.bearer()))
                .bodyJson().satisfies(json -> {
                    assertThat(json).extractingPath("$.page.totalElements").isEqualTo(1);
                    assertThat(json).extractingPath("$.content[0].gameDate").isEqualTo(gameDay.toString());
                    assertThat(json).extractingPath("$.content[0].model").isEqualTo("logistic-regression");
                });
        assertThat(mvc.get().uri("/api/v1/predictions/mine").header(HttpHeaders.AUTHORIZATION, bob.bearer()))
                .bodyJson().extractingPath("$.page.totalElements").isEqualTo(0);
    }

    @Test
    void aCardSavedWithAnotherFeatureListIsNeverApplied() {
        cards.save(obsoleteCard());
        Session alice = TestAccounts.register(mvc);

        assertThat(explain(alice)).hasStatus(409).bodyJson().extractingPath("$.code").isEqualTo("MODEL_NOT_TRAINED");
        assertThat(cards.current()).isEmpty();
    }

    private MvcTestResult explain(Session user) {
        return mvc.get().uri("/api/v1/spark/ml/predict/game/explanation")
                .param("seasonId", String.valueOf(seasonId))
                .param("homeTeamId", String.valueOf(homeTeamId))
                .param("awayTeamId", String.valueOf(awayTeamId))
                .param("date", gameDay.toString())
                .header(HttpHeaders.AUTHORIZATION, user.bearer())
                .exchange();
    }

    /** Une carte d'une ancienne version du modèle, avec une variable qui n'existe plus. */
    private static ScoringCard obsoleteCard() {
        return new ScoringCard("logistic-regression", "v-old", List.of("point_diff_diff"),
                new double[]{0}, new double[]{1}, new double[]{1}, 0);
    }

    private long create(String uri, String body) throws Exception {
        MvcTestResult result = mvc.post().uri(uri).header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON).content(body).exchange();
        assertThat(result).as("POST %s", uri).hasStatus(201);
        return JsonPath.<Number>read(result.getResponse().getContentAsString(), "$.id").longValue();
    }

    private static String team(String name) {
        String abbreviation = "Q" + Long.toString(ThreadLocalRandom.current().nextLong(1L << 30), 36).toUpperCase();
        return """
                {"name":"%s","city":"Testville","abbreviation":"%s","conference":"EASTERN","division":"ATLANTIC"}
                """.formatted(name, abbreviation);
    }
}
