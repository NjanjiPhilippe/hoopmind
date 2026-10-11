package com.np3tech.hoopmind.shared.job;

import com.np3tech.hoopmind.shared.exception.AppException;
import com.np3tech.hoopmind.shared.job.Job.Requester;
import com.np3tech.hoopmind.shared.job.JobState.Failed;
import com.np3tech.hoopmind.shared.job.JobState.Interrupted;
import com.np3tech.hoopmind.shared.job.JobState.Running;
import com.np3tech.hoopmind.shared.job.JobState.Succeeded;
import com.np3tech.hoopmind.shared.lock.DataOperationLock;
import com.np3tech.hoopmind.shared.lock.DataOperationLock.Permit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Lance des opérations longues en arrière-plan et garde leur état en base ({@code background_jobs}).
 * <p>
 * Chaque job prend le verrou des données de son type ({@link DataOperationLock}) avant d'être enregistré : une
 * opération concurrente reçoit un 409 et rien n'est créé. Il s'exécute sur l'exécuteur de tâches de Spring (fils
 * virtuels, arrêt propre de l'application). Au démarrage, un job resté « en cours » est marqué interrompu :
 * le processus qui l'exécutait n'existe plus.
 */
@Slf4j
@Service
public class JobService {

    private static final TypeReference<Map<String, String>> PARAMETERS = new TypeReference<>() {
    };

    private final JdbcClient jdbc;
    private final JsonMapper json;
    private final AsyncTaskExecutor executor;
    private final DataOperationLock lock;
    private final Clock clock;

    /** Avancement des jobs en cours de ce processus (le reste de l'état est en base). */
    private final ConcurrentMap<UUID, Running<?>> progress = new ConcurrentHashMap<>();

    public JobService(JdbcClient jdbc, JsonMapper json, @Qualifier("applicationTaskExecutor") AsyncTaskExecutor executor,
                      DataOperationLock lock, Clock clock) {
        this.jdbc = jdbc;
        this.json = json;
        this.executor = executor;
        this.lock = lock;
        this.clock = clock;
    }

    /**
     * Lance un job, ou lève une {@link AppException} 409 si une autre opération sur les données est en cours.
     *
     * @param firstStep étape affichée dès le lancement
     */
    public <R> Job<R> start(JobType type, Requester requestedBy, Map<String, String> parameters, String firstStep,
                            JobBody<R> body) {
        Permit permit = lock.acquire(type.lock());
        UUID id = UUID.randomUUID();
        boolean recorded = false;
        try {
            Instant now = clock.instant();
            jdbc.sql("""
                            INSERT INTO background_jobs (id, type, state, step, requested_by_id, requested_by_login,
                                                         parameters, created_at)
                            VALUES (?, ?, 'RUNNING', ?, ?, ?, CAST(? AS JSONB), ?)""")
                    .params(id, type.name(), firstStep, requestedBy == null ? null : requestedBy.id(),
                            requestedBy == null ? null : requestedBy.login(), json.writeValueAsString(parameters),
                            Timestamp.from(now))
                    .update();
            recorded = true;
            Running<R> running = new Running<>(firstStep, null, null);
            progress.put(id, running);
            executor.execute(() -> run(id, type, body, permit));
            return new Job<>(id, type, requestedBy, parameters, now, running);
        } catch (RuntimeException e) {
            permit.close();
            progress.remove(id);
            if (recorded) {
                fail(id, e);
            }
            throw e;
        }
    }

    /** Le dernier job de ce type, s'il y en a un. */
    public <R> Optional<Job<R>> latest(JobType type, Class<R> resultType) {
        return jdbc.sql("SELECT * FROM background_jobs WHERE type = ? ORDER BY created_at DESC LIMIT 1")
                .param(type.name())
                .query((rs, row) -> this.<R>job(rs, resultType))
                .optional();
    }

    public <R> Optional<Job<R>> find(UUID id, Class<R> resultType) {
        return jdbc.sql("SELECT * FROM background_jobs WHERE id = ?")
                .param(id)
                .query((rs, row) -> this.<R>job(rs, resultType))
                .optional();
    }

    /** Message d'erreur présentable : celui d'une {@link AppException}, sinon un message générique. */
    public static String userMessage(Throwable error) {
        return error instanceof AppException ? error.getMessage()
                : "Erreur inattendue. Le détail est dans les journaux du serveur.";
    }

    @EventListener(ApplicationReadyEvent.class)
    void markInterruptedJobs() {
        int interrupted = jdbc.sql("""
                        UPDATE background_jobs SET state = 'INTERRUPTED', finished_at = ?
                        WHERE state = 'RUNNING'""")
                .param(Timestamp.from(clock.instant()))
                .update();
        if (interrupted > 0) {
            log.warn("{} job(s) interrompu(s) par l'arrêt précédent du serveur", interrupted);
        }
    }

    private <R> void run(UUID id, JobType type, JobBody<R> body, Permit permit) {
        try (permit) {
            R result = body.run(new Context(id));
            jdbc.sql("UPDATE background_jobs SET state = 'SUCCEEDED', finished_at = ?, result = CAST(? AS JSONB) WHERE id = ?")
                    .params(Timestamp.from(clock.instant()), json.writeValueAsString(result), id)
                    .update();
        } catch (RuntimeException e) {
            log.error("Échec du job {} {}", type, id, e);
            fail(id, e);
        } finally {
            progress.remove(id);
        }
    }

    private void fail(UUID id, RuntimeException error) {
        jdbc.sql("UPDATE background_jobs SET state = 'FAILED', finished_at = ?, error = ? WHERE id = ?")
                .params(Timestamp.from(clock.instant()), truncate(userMessage(error)), id)
                .update();
    }

    private <R> Job<R> job(ResultSet rs, Class<R> resultType) throws SQLException {
        UUID id = rs.getObject("id", UUID.class);
        Long requesterId = rs.getObject("requested_by_id", Long.class);
        String requesterLogin = rs.getString("requested_by_login");
        Requester requester = requesterId == null && requesterLogin == null ? null : new Requester(requesterId, requesterLogin);
        String step = rs.getString("step");
        Instant finishedAt = instant(rs, "finished_at");
        JobState<R> state = switch (rs.getString("state")) {
            case "RUNNING" -> {
                @SuppressWarnings("unchecked")
                Running<R> live = (Running<R>) progress.get(id);
                yield live != null ? live : new Running<>(step, null, null);
            }
            case "SUCCEEDED" -> new Succeeded<>(finishedAt, json.readValue(rs.getString("result"), resultType));
            case "FAILED" -> new Failed<>(finishedAt, step, rs.getString("error"));
            case "INTERRUPTED" -> new Interrupted<>(finishedAt, step);
            default -> throw new IllegalStateException("État de job inconnu : " + rs.getString("state"));
        };
        return new Job<>(id, JobType.valueOf(rs.getString("type")), requester,
                json.readValue(rs.getString("parameters"), PARAMETERS), instant(rs, "created_at"), state);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static String truncate(String message) {
        return message == null || message.length() <= 500 ? message : message.substring(0, 500);
    }

    private final class Context implements JobContext {

        private final UUID id;

        Context(UUID id) {
            this.id = id;
        }

        @Override
        public UUID jobId() {
            return id;
        }

        @Override
        public void step(String step) {
            progress.put(id, new Running<>(step, null, null));
            jdbc.sql("UPDATE background_jobs SET step = ? WHERE id = ?").params(step, id).update();
        }

        @Override
        public void progress(Long done, Long total) {
            progress.computeIfPresent(id, (key, running) -> new Running<>(running.step(), done, total));
        }
    }
}
