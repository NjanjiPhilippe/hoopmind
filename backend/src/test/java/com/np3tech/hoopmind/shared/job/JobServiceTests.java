package com.np3tech.hoopmind.shared.job;

import com.np3tech.hoopmind.IntegrationTest;
import com.np3tech.hoopmind.shared.exception.AppException;
import com.np3tech.hoopmind.shared.job.JobState.Failed;
import com.np3tech.hoopmind.shared.job.JobState.Interrupted;
import com.np3tech.hoopmind.shared.job.JobState.Succeeded;
import com.np3tech.hoopmind.shared.lock.DataOperationLock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
class JobServiceTests {

    @Autowired
    JobService jobs;

    @Autowired
    DataOperationLock lock;

    @Autowired
    JdbcTemplate jdbc;

    record Result(int value) {
    }

    @Test
    void runsInTheBackgroundAndKeepsTheResult() throws Exception {
        CountDownLatch progressReported = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Job<Result> started = jobs.start(JobType.MODEL_TRAINING, new Job.Requester(1L, "admin"), Map.of("seasons", "3"),
                "TRAINING", context -> {
                    context.progress(1L, 2L);
                    progressReported.countDown();
                    await(release);
                    return new Result(42);
                });

        // Le job tourne sur un autre fil : on attend qu'il ait signalé son avancement avant de le lire, et on le
        // libère quoi qu'il arrive, sinon il garde le verrou des données et fait échouer les tests suivants (409).
        try {
            assertThat(started.state()).isInstanceOf(JobState.Running.class);
            assertThat(progressReported.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(jobs.find(started.id(), Result.class)).hasValueSatisfying(job -> {
                assertThat(job.state()).isEqualTo(new JobState.Running<Result>("TRAINING", 1L, 2L));
                assertThat(job.requestedBy()).isEqualTo(new Job.Requester(1L, "admin"));
                assertThat(job.parameters()).containsEntry("seasons", "3");
            });
            assertThatThrownBy(() -> jobs.start(JobType.NBA_DATABASE_IMPORT, null, Map.of(), "IMPORTING", c -> null))
                    .isInstanceOfSatisfying(AppException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        } finally {
            release.countDown();
        }
        Job<Result> done = waitForEnd(started.id());
        assertThat(done.state()).isInstanceOfSatisfying(Succeeded.class,
                succeeded -> assertThat(succeeded.result()).isEqualTo(new Result(42)));
    }

    @Test
    void anUnexpectedErrorIsRecordedWithoutItsTechnicalMessage() throws Exception {
        Job<Result> started = jobs.start(JobType.MODEL_TRAINING, null, Map.of(), "TRAINING", context -> {
            context.step("SCORING");
            throw new IllegalStateException("/srv/secret/path: disk full");
        });

        Job<Result> done = waitForEnd(started.id());
        assertThat(done.state()).isInstanceOfSatisfying(Failed.class, failed -> {
            assertThat(failed.step()).isEqualTo("SCORING");
            assertThat(failed.error()).doesNotContain("/srv").contains("journaux du serveur");
        });
    }

    @Test
    void jobsLeftRunningByAStoppedServerAreMarkedInterruptedAtStartup() {
        UUID orphan = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO background_jobs (id, type, state, step, parameters, created_at)
                VALUES (?, 'NBA_DATABASE_IMPORT', 'RUNNING', 'IMPORTING', '{}'::jsonb, ?)""",
                orphan, Timestamp.from(Instant.now()));

        jobs.markInterruptedJobs();

        assertThat(jobs.find(orphan, Result.class)).hasValueSatisfying(job -> assertThat(job.state())
                .isInstanceOfSatisfying(Interrupted.class, interrupted -> assertThat(interrupted.step()).isEqualTo("IMPORTING")));
    }

    private Job<Result> waitForEnd(UUID id) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        while (Instant.now().isBefore(deadline)) {
            Job<Result> job = jobs.find(id, Result.class).orElseThrow();
            if (!job.state().running() && lock.current().isEmpty()) {
                return job;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("job toujours en cours");
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
