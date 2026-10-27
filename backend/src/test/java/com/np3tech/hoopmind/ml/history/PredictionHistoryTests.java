package com.np3tech.hoopmind.ml.history;

import com.jayway.jsonpath.JsonPath;
import com.np3tech.hoopmind.IntegrationTest;
import com.np3tech.hoopmind.TestAccounts;
import com.np3tech.hoopmind.TestAccounts.Session;
import com.np3tech.hoopmind.ml.GameOutcomePrediction;
import com.np3tech.hoopmind.ml.ScorePrediction;
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
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * « Mes prédictions » : chacun ne voit et ne retire que les siennes, avec le score réel une fois le match joué.
 */
@IntegrationTest
class PredictionHistoryTests {

    private static final LocalDate GAME_DAY = LocalDate.of(2025, 1, 10);

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PredictionHistory history;

    private String admin;
    private long seasonId;
    private long homeTeamId;
    private long awayTeamId;

    @BeforeEach
    void createMatch() throws Exception {
        admin = TestAccounts.admin(mvc).bearer();
        int year = ThreadLocalRandom.current().nextInt(3000, 9000);
        seasonId = create("/api/v1/seasons", """
                {"year":%d,"name":"%d-%d","startDate":"%d-10-20","endDate":"%d-04-15","status":"IN_PROGRESS"}
                """.formatted(year, year, (year + 1) % 100, year, year + 1));
        homeTeamId = create("/api/v1/teams", team("Home"));
        awayTeamId = create("/api/v1/teams", team("Away"));
        long gameId = create("/api/v1/games", """
                {"homeTeamId":%d,"awayTeamId":%d,"seasonId":%d,"gameType":"REGULAR_SEASON",
                 "scheduledDate":"%sT19:30:00","venue":"Salle de test"}
                """.formatted(homeTeamId, awayTeamId, seasonId, GAME_DAY));
        assertThat(mvc.put().uri("/api/v1/games/{id}/result", gameId).header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"homeTeamScore":110,"awayTeamScore":104}
                        """)).hasStatusOk();
    }

    @AfterEach
    void deleteMatch() {
        jdbc.update("DELETE FROM game_results WHERE game_id IN (SELECT id FROM games WHERE season_id = ?)", seasonId);
        jdbc.update("DELETE FROM games WHERE season_id = ?", seasonId);
        jdbc.update("DELETE FROM teams WHERE id IN (?, ?)", homeTeamId, awayTeamId);
        jdbc.update("DELETE FROM seasons WHERE id = ?", seasonId);
    }

    @Test
    void eachUserSeesOnlyTheirOwnPredictionsWithTheActualResult() {
        Session alice = TestAccounts.register(mvc);
        Session bob = TestAccounts.register(mvc);
        history.record(alice.userId(), match(GAME_DAY), prediction(0.7, 112, 105));
        history.record(alice.userId(), match(GAME_DAY.plusDays(3)), prediction(0.4, null, null));
        history.record(bob.userId(), match(GAME_DAY), prediction(0.3, 101, 108));

        MvcTestResult mine = mvc.get().uri("/api/v1/predictions/mine").header(HttpHeaders.AUTHORIZATION, alice.bearer()).exchange();
        assertThat(mine).hasStatusOk();
        assertThat(mine).bodyJson().extractingPath("$.page.totalElements").isEqualTo(2);
        // Les plus récentes d'abord : le match pas encore joué, puis celui du 10 janvier.
        assertThat(mine).bodyJson().doesNotHavePath("$.content[0].actualHomePoints");
        assertThat(mine).bodyJson().doesNotHavePath("$.content[0].correct");
        assertThat(mine).bodyJson().extractingPath("$.content[1].homeTeam").isEqualTo("Testville Home");
        assertThat(mine).bodyJson().extractingPath("$.content[1].predictedHomePoints").isEqualTo(112);
        assertThat(mine).bodyJson().extractingPath("$.content[1].actualHomePoints").isEqualTo(110);
        assertThat(mine).bodyJson().extractingPath("$.content[1].correct").isEqualTo(true);

        MvcTestResult theirs = mvc.get().uri("/api/v1/predictions/mine").header(HttpHeaders.AUTHORIZATION, bob.bearer()).exchange();
        assertThat(theirs).bodyJson().extractingPath("$.page.totalElements").isEqualTo(1);
        assertThat(theirs).bodyJson().extractingPath("$.content[0].correct").isEqualTo(false);
    }

    @Test
    void predictingTheSameMatchAgainReplacesThePreviousLine() {
        Session alice = TestAccounts.register(mvc);
        history.record(alice.userId(), match(GAME_DAY), prediction(0.55, null, null));
        history.record(alice.userId(), match(GAME_DAY), prediction(0.62, 109, 103));

        MvcTestResult mine = mvc.get().uri("/api/v1/predictions/mine").header(HttpHeaders.AUTHORIZATION, alice.bearer()).exchange();
        assertThat(mine).bodyJson().extractingPath("$.page.totalElements").isEqualTo(1);
        assertThat(mine).bodyJson().extractingPath("$.content[0].homeWinProbability").isEqualTo(0.62);
    }

    @Test
    void nobodyDeletesSomeoneElsesPrediction() throws Exception {
        Session alice = TestAccounts.register(mvc);
        Session bob = TestAccounts.register(mvc);
        history.record(alice.userId(), match(GAME_DAY), prediction(0.7, null, null));
        MvcTestResult mine = mvc.get().uri("/api/v1/predictions/mine").header(HttpHeaders.AUTHORIZATION, alice.bearer()).exchange();
        long id = JsonPath.<Number>read(mine.getResponse().getContentAsString(), "$.content[0].id").longValue();

        assertThat(mvc.delete().uri("/api/v1/predictions/mine/{id}", id).header(HttpHeaders.AUTHORIZATION, bob.bearer()))
                .hasStatus(404);
        assertThat(mvc.delete().uri("/api/v1/predictions/mine/{id}", id).header(HttpHeaders.AUTHORIZATION, alice.bearer()))
                .hasStatus(204);
        assertThat(mvc.get().uri("/api/v1/predictions/mine").header(HttpHeaders.AUTHORIZATION, alice.bearer()))
                .bodyJson().extractingPath("$.page.totalElements").isEqualTo(0);
    }

    private PredictedMatch match(LocalDate day) {
        return new PredictedMatch(seasonId, homeTeamId, awayTeamId, day, false);
    }

    private static GameOutcomePrediction prediction(double homeWinProbability, Integer home, Integer away) {
        ScorePrediction score = home == null ? null : ScorePrediction.of(home, away, 10, 110, 108, 107, 109);
        return new GameOutcomePrediction(homeWinProbability >= 0.5, homeWinProbability, 1 - homeWinProbability,
                "game-outcome-logistic-regression", score);
    }

    private static String team(String name) {
        String abbreviation = "Q" + Long.toString(ThreadLocalRandom.current().nextLong(1L << 30), 36).toUpperCase();
        return """
                {"name":"%s","city":"Testville","abbreviation":"%s","conference":"EASTERN","division":"ATLANTIC"}
                """.formatted(name, abbreviation);
    }

    private long create(String uri, String body) throws Exception {
        MvcTestResult result = mvc.post().uri(uri).header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON).content(body).exchange();
        assertThat(result).as("POST %s", uri).hasStatus(201);
        return JsonPath.<Number>read(result.getResponse().getContentAsString(), "$.id").longValue();
    }
}
