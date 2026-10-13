package com.np3tech.hoopmind.ingestion.nbadb;

import com.np3tech.hoopmind.IntegrationTest;
import com.np3tech.hoopmind.ingestion.DataImportEvent;
import com.np3tech.hoopmind.ingestion.nbadb.StatsNbaJobService.StatsNbaJobStatus;
import com.np3tech.hoopmind.ingestion.nbadb.StatsNbaJobService.StatsNbaPhase;
import com.np3tech.hoopmind.nba.repository.SeasonRepository;
import com.np3tech.hoopmind.nba.repository.TeamRepository;
import com.np3tech.hoopmind.shared.job.JobService;
import com.np3tech.hoopmind.shared.lock.DataOperationLock;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.np3tech.hoopmind.ingestion.nbadb.StatsNbaFixture.BOS;
import static com.np3tech.hoopmind.ingestion.nbadb.StatsNbaFixture.LAL;
import static com.np3tech.hoopmind.ingestion.nbadb.StatsNbaFixture.NYK;
import static com.np3tech.hoopmind.ingestion.nbadb.StatsNbaFixture.POR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Le bouton « Compléter avec stats.nba.com » : lecture de chaque saison puis import, en arrière-plan.
 * Les saisons (2014-15, 2015-16) ne croisent pas celles des autres tests, qui partagent la base.
 */
@IntegrationTest
class StatsNbaJobTests {

    /** Le 1er mars 2016, la saison en cours est 2015-16. */
    private static final Clock MARCH_2016 = Clock.fixed(Instant.parse("2016-03-01T12:00:00Z"), ZoneOffset.UTC);

    @Autowired
    TeamRepository teams;

    @Autowired
    SeasonRepository seasons;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    JobService jobService;

    @Autowired
    DataOperationLock lock;

    @Test
    void readsEachSeasonFromStatsNbaThenImportsThem() throws Exception {
        Map<String, String> responses = Map.of(
                "Season=2014-15&SeasonType=Regular+Season", new StatsNbaFixture()
                        .played("0021400001", "2014-10-28", BOS, NYK, 121, 105).json(),
                "Season=2015-16&SeasonType=Regular+Season", new StatsNbaFixture()
                        .played("0021500001", "2015-10-27", BOS, NYK, 95, 112)
                        .played("0021500002", "2015-10-28", LAL, POR, 99, 101).json(),
                "Season=2015-16&SeasonType=Playoffs", new StatsNbaFixture().json());
        String empty = new StatsNbaFixture().json();
        List<String> requested = new CopyOnWriteArrayList<>();
        HttpServer stats = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        stats.createContext("/stats/leaguegamelog", exchange -> {
            String query = exchange.getRequestURI().getRawQuery();
            requested.add(query);
            byte[] body = responses.entrySet().stream().filter(e -> query.contains(e.getKey())).findFirst()
                    .map(Map.Entry::getValue).orElse(empty).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        stats.start();
        try {
            StatsNbaProperties properties = new StatsNbaProperties(
                    "http://127.0.0.1:" + stats.getAddress().getPort() + "/stats", 0L);
            NbaDatabaseImportProperties importProperties = new NbaDatabaseImportProperties(null, 10, false);
            List<Object> events = new CopyOnWriteArrayList<>();
            StatsNbaJobService jobs = new StatsNbaJobService(
                    new StatsNbaClient(properties, JsonMapper.builder().build()), properties,
                    new NbaDatabaseImporter(importProperties, teams, seasons, jdbc, MARCH_2016), importProperties,
                    seasons, jobService, events::add, MARCH_2016);

            assertThat(jobs.status().toSeason()).isEqualTo("2015-16");
            assertThatThrownBy(() -> jobs.start(2016)).isInstanceOf(NbaImportException.class);

            StatsNbaJobStatus started = jobs.start(2014);
            assertThat(started.phase()).isEqualTo(StatsNbaPhase.FETCHING);
            assertThat(started.fromSeason()).isEqualTo("2014-15");

            StatsNbaJobStatus done = waitForEnd(jobs, lock);
            assertThat(done.phase()).as(done.error()).isEqualTo(StatsNbaPhase.SUCCEEDED);
            assertThat(requested).hasSize(4);
            assertThat(done.report().seasons()).containsExactly("2014-15", "2015-16");
            assertThat(done.report().games()).isEqualTo(3);
            assertThat(done.report().teamGameStats()).isEqualTo(6);
            assertThat(events).containsExactly(new DataImportEvent.StatsNbaImported(null, List.of("2014-15", "2015-16"), 3));

            assertThat(jdbc.queryForMap("""
                    SELECT h.abbreviation AS home, w.abbreviation AS winner, r.home_team_score, s.year
                    FROM games g JOIN game_results r ON r.game_id = g.id JOIN teams h ON h.id = g.home_team_id
                    JOIN teams w ON w.id = r.winner_id JOIN seasons s ON s.id = g.season_id
                    WHERE g.external_id = '0021500002'"""))
                    .containsEntry("home", "LAL").containsEntry("winner", "POR")
                    .containsEntry("home_team_score", 99).containsEntry("year", 2015);
            assertThat(teams.findByAbbreviation("POR")).get()
                    .satisfies(team -> assertThat(team.getExternalId()).isEqualTo(POR));
        } finally {
            stats.stop(0);
        }
    }

    private static StatsNbaJobStatus waitForEnd(StatsNbaJobService jobs, DataOperationLock lock)
            throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(30));
        while (Instant.now().isBefore(deadline)) {
            StatsNbaJobStatus job = jobs.status().job();
            if ((job.phase() == StatsNbaPhase.SUCCEEDED || job.phase() == StatsNbaPhase.FAILED)
                    && lock.current().isEmpty()) {
                return job;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("import toujours en cours");
    }
}
