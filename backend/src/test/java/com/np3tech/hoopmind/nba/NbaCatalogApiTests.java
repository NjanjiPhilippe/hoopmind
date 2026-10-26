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

import java.util.List;
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

    @Autowired
    GameHistory history;

    private String admin;
    private int seasonYear;
    private long seasonId;
    private long homeTeamId;
    private long awayTeamId;

    @BeforeEach
    void createCatalog() throws Exception {
        admin = TestAccounts.admin(mvc).bearer();
        // La base est partagée par les tests d'intégration : année et abréviations tirées au hasard.
        int year = ThreadLocalRandom.current().nextInt(3000, 9000);
        seasonYear = year;
        seasonId = create("/api/v1/seasons", """
                {"year":%d,"name":"%d-%d","startDate":"%d-10-20","endDate":"%d-04-15","status":"IN_PROGRESS"}
                """.formatted(year, year, (year + 1) % 100, year, year + 1));
        homeTeamId = create("/api/v1/teams", team("Home"));
        awayTeamId = create("/api/v1/teams", team("Away"));
    }

    /** D'autres tests comptent les 30 équipes de référence : on retire tout ce que ce test a créé. */
    @AfterEach
    void deleteCatalog() {
        jdbc.update("DELETE FROM game_results WHERE game_id IN (SELECT id FROM games WHERE season_id = ?)", seasonId);
        jdbc.update("DELETE FROM games WHERE season_id = ?", seasonId);
        jdbc.update("DELETE FROM player_stats WHERE season_id = ?", seasonId);
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

        MvcTestResult page = mvc.get().uri("/api/v1/players?size=2").exchange();

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
    void anAdminEntersAndCorrectsAScoreThatTheFormUsesRightAway() throws Exception {
        long gameId = create("/api/v1/games", game(homeTeamId, awayTeamId));

        MvcTestResult recorded = putResult(gameId, """
                {"homeTeamScore":98,"awayTeamScore":104,"overtimePeriods":1}
                """);
        assertThat(recorded).hasStatusOk();
        assertThat(recorded).bodyJson().extractingPath("$.status").isEqualTo("COMPLETED");
        assertThat(recorded).bodyJson().extractingPath("$.result.winner.id").isEqualTo((int) awayTeamId);
        assertThat(recorded).bodyJson().extractingPath("$.result.overtime").isEqualTo(true);

        // Correction : le même match, un autre score, toujours un seul résultat.
        assertThat(putResult(gameId, """
                {"homeTeamScore":110,"awayTeamScore":104}
                """)).hasStatusOk().bodyJson().extractingPath("$.result.winner.id").isEqualTo((int) homeTeamId);
        GameRecord game = history.teamGames(seasonYear, List.of(homeTeamId)).getFirst();
        assertThat(game.played()).isTrue();
        assertThat(game.homePoints()).isEqualTo(110);

        assertThat(mvc.delete().uri("/api/v1/games/{id}/result", gameId).header(HttpHeaders.AUTHORIZATION, admin)
                .exchange()).hasStatus(204);
        assertThat(mvc.get().uri("/api/v1/games/{id}", gameId).exchange())
                .bodyJson().extractingPath("$.status").isEqualTo("SCHEDULED");
        assertThat(history.teamGames(seasonYear, List.of(homeTeamId)).getFirst().played()).isFalse();
    }

    @Test
    void aScoreCannotBeATieAndOnlyAnAdminEntersIt() throws Exception {
        long gameId = create("/api/v1/games", game(homeTeamId, awayTeamId));

        assertThat(putResult(gameId, """
                {"homeTeamScore":100,"awayTeamScore":100}
                """)).hasStatus(400).bodyJson().extractingPath("$.code").isEqualTo("INVALID_GAME_RESULT");
        assertThat(mvc.put().uri("/api/v1/games/{id}/result", gameId)
                .header(HttpHeaders.AUTHORIZATION, TestAccounts.register(mvc).bearer())
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"homeTeamScore":100,"awayTeamScore":90}
                        """).exchange()).hasStatus(403);
    }

    @Test
    void anAdminEntersAPlayerAndTheirSeasonStats() throws Exception {
        long playerId = create("/api/v1/players", """
                {"firstName":"Joueur","lastName":"Saisi","position":"POINT_GUARD","jerseyNumber":7,"teamId":%d,"height":191}
                """.formatted(homeTeamId));

        assertThat(putStats(playerId, admin, """
                {"gamesPlayed":70,"gamesStarted":68,"pointsPerGame":24.6,"assistsPerGame":8.1,"fieldGoalPercentage":0.475}
                """)).hasStatusOk().bodyJson().extractingPath("$.season.id").isEqualTo((int) seasonId);
        // Une nouvelle saisie remplace la précédente.
        assertThat(putStats(playerId, admin, """
                {"gamesPlayed":72,"pointsPerGame":25.0}
                """)).hasStatusOk();

        MvcTestResult stats = mvc.get().uri("/api/v1/players/{id}/stats", playerId).exchange();
        assertThat(stats).hasStatusOk();
        assertThat(stats).bodyJson().extractingPath("$.length()").isEqualTo(1);
        assertThat(stats).bodyJson().extractingPath("$[0].gamesPlayed").isEqualTo(72);
        assertThat(stats).bodyJson().extractingPath("$[0].pointsPerGame").isEqualTo(25.0);

        assertThat(mvc.put().uri("/api/v1/players/{id}", playerId).header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"status":"INJURED","jerseyNumber":11}
                        """)).hasStatusOk().bodyJson().extractingPath("$.status").isEqualTo("INJURED");

        // Supprimer le joueur supprime ses statistiques.
        assertThat(mvc.delete().uri("/api/v1/players/{id}", playerId).header(HttpHeaders.AUTHORIZATION, admin)).hasStatus(204);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM player_stats WHERE player_id = ?", Integer.class, playerId)).isZero();
    }

    @Test
    void playerStatsAreValidatedAndOnlyAnAdminEntersThem() throws Exception {
        long playerId = create("/api/v1/players", """
                {"firstName":"Joueur","lastName":"Contrôlé","position":"CENTER","teamId":%d}
                """.formatted(homeTeamId));

        assertThat(putStats(playerId, admin, """
                {"gamesPlayed":10,"gamesStarted":12}
                """)).hasStatus(400).bodyJson().extractingPath("$.code").isEqualTo("INVALID_PLAYER_STATS");
        assertThat(putStats(playerId, admin, """
                {"gamesPlayed":10,"fieldGoalPercentage":47.5}
                """)).hasStatus(400);
        assertThat(putStats(playerId, TestAccounts.register(mvc).bearer(), """
                {"gamesPlayed":10}
                """)).hasStatus(403);
        assertThat(mvc.post().uri("/api/v1/players").header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"firstName":"Joueur","lastName":"Numéro","position":"CENTER","teamId":%d,"jerseyNumber":120}
                        """.formatted(homeTeamId))).hasStatus(400);
    }

    private MvcTestResult putStats(long playerId, String bearer, String body) {
        return mvc.put().uri("/api/v1/players/{id}/stats/{seasonId}", playerId, seasonId).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content(body).exchange();
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

    private MvcTestResult putResult(long gameId, String body) {
        return mvc.put().uri("/api/v1/games/{id}/result", gameId).header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    private long create(String uri, String body) throws Exception {
        MvcTestResult result = mvc.post().uri(uri).header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON).content(body).exchange();
        assertThat(result).as("POST %s", uri).hasStatus(201);
        return JsonPath.<Number>read(result.getResponse().getContentAsString(), "$.id").longValue();
    }
}
