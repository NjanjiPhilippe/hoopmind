package com.np3tech.hoopmind.ingestion.nbadb;

import com.np3tech.hoopmind.IntegrationTest;
import com.np3tech.hoopmind.ingestion.DataImportEvent;
import com.np3tech.hoopmind.ingestion.nbadb.EoinMooreJobService.EoinMooreJobStatus;
import com.np3tech.hoopmind.ingestion.nbadb.EoinMooreJobService.EoinMoorePhase;
import com.np3tech.hoopmind.nba.repository.SeasonRepository;
import com.np3tech.hoopmind.nba.repository.TeamRepository;
import com.np3tech.hoopmind.shared.job.JobService;
import com.np3tech.hoopmind.shared.lock.DataOperationLock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.np3tech.hoopmind.ingestion.nbadb.EoinMooreFixture.BOS;
import static com.np3tech.hoopmind.ingestion.nbadb.EoinMooreFixture.LAL;
import static com.np3tech.hoopmind.ingestion.nbadb.EoinMooreFixture.NYK;
import static com.np3tech.hoopmind.ingestion.nbadb.EoinMooreFixture.POR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L'import des fichiers d'Eoin Moore, en arrière-plan. La saison (2010-11) ne croise pas celles des autres tests,
 * qui partagent la base.
 */
@IntegrationTest
class EoinMooreJobTests {

    private static final int[] WIN = {30, 30, 30, 30};
    private static final int[] LOSS = {25, 25, 25, 25};

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

    @TempDir
    Path directory;

    @Test
    void importsTheRequestedSeasonWithPlayInGamesKeptOutOfTheRecord() throws Exception {
        List<Object> events = new CopyOnWriteArrayList<>();
        EoinMooreJobService jobs = jobs(events);
        assertThat(jobs.status().games().present()).isFalse();
        assertThatThrownBy(() -> jobs.start(2010, 2010)).isInstanceOf(NbaImportException.class)
                .hasMessageContaining("Games.csv");

        new EoinMooreFixture()
                .played("21000001", "2010-10-26", BOS, NYK, WIN, LOSS)
                .played("21000002", "2010-10-27", LAL, POR, LOSS, WIN)
                .played("51000101", "2011-04-12", LAL, POR, WIN, LOSS)
                .played("41000101", "2011-04-16", BOS, NYK, new int[]{25, 25, 25, 25, 12}, new int[]{25, 25, 25, 25, 10})
                .scheduled("21000003", "2011-04-13", NYK, BOS)
                .write(directory);
        assertThat(jobs.status().teamStatistics().present()).isTrue();

        EoinMooreJobStatus started = jobs.start(2010, null);
        assertThat(started.fromSeason()).isEqualTo("2010-11");
        assertThat(started.toSeason()).isEqualTo("2010-11");

        EoinMooreJobStatus done = waitForEnd(jobs);
        assertThat(done.phase()).as(done.error()).isEqualTo(EoinMoorePhase.SUCCEEDED);
        assertThat(done.report().seasons()).containsExactly("2010-11");
        assertThat(done.report().games()).isEqualTo(5);
        assertThat(done.report().playedGames()).isEqualTo(4);
        assertThat(events).containsExactly(new DataImportEvent.EoinMooreImported(null, List.of("2010-11"), 5));

        assertThat(jdbc.queryForObject("SELECT game_type FROM games WHERE external_id = '0051000101'", String.class))
                .isEqualTo("PLAY_IN");
        assertThat(jdbc.queryForMap("""
                SELECT g.status, r.overtime_periods, r.home_team_quarter_scores_json AS quarters
                FROM games g JOIN game_results r ON r.game_id = g.id WHERE g.external_id = '0041000101'"""))
                .containsEntry("status", "COMPLETED").containsEntry("overtime_periods", 1)
                .containsEntry("quarters", "[25,25,25,25,12]");
        assertThat(jdbc.queryForObject("SELECT status FROM games WHERE external_id = '0021000003'", String.class))
                .isEqualTo("SCHEDULED");
        // Le play-in gagné par les Lakers ne compte pas dans leur bilan de saison régulière (0-1).
        assertThat(jdbc.queryForMap("""
                SELECT ts.wins, ts.losses FROM team_stats ts JOIN teams t ON t.id = ts.team_id
                JOIN seasons s ON s.id = ts.season_id WHERE t.abbreviation = 'LAL' AND s.year = 2010"""))
                .containsEntry("wins", 0).containsEntry("losses", 1);
    }

    private EoinMooreJobService jobs(List<Object> events) {
        NbaDatabaseImportProperties importProperties = new NbaDatabaseImportProperties(null, 10, false);
        return new EoinMooreJobService(new NbaDatabaseImporter(importProperties, teams, seasons, jdbc, Clock.systemUTC()),
                new EoinMooreProperties(directory.toString()), jdbc, jobService, events::add);
    }

    private EoinMooreJobStatus waitForEnd(EoinMooreJobService jobs) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(30));
        while (Instant.now().isBefore(deadline)) {
            EoinMooreJobStatus job = jobs.status().job();
            if ((job.phase() == EoinMoorePhase.SUCCEEDED || job.phase() == EoinMoorePhase.FAILED)
                    && lock.current().isEmpty()) {
                return job;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("import toujours en cours");
    }
}
