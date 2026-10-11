package com.np3tech.hoopmind.ml.training;

import com.np3tech.hoopmind.auth.CurrentUser;
import com.np3tech.hoopmind.ml.GameOutcomeModel;
import com.np3tech.hoopmind.ml.ModelTrainingEvent;
import com.np3tech.hoopmind.ml.TrainingReport;
import com.np3tech.hoopmind.shared.exception.AppException;
import com.np3tech.hoopmind.shared.job.Job;
import com.np3tech.hoopmind.shared.job.JobService;
import com.np3tech.hoopmind.shared.job.JobState;
import com.np3tech.hoopmind.shared.job.JobType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Entraîne le modèle de prédiction en arrière-plan : l'entraînement Spark dure de quelques secondes à plusieurs
 * minutes, la requête HTTP rend la main tout de suite et l'interface suit l'avancement.
 */
@Service
public class ModelTrainingJobs {

    public enum Phase { TRAINING, SUCCEEDED, FAILED, INTERRUPTED }

    /** @param seasons nombre de saisons demandé, {@code null} pour le réglage par défaut */
    public record TrainingJobStatus(UUID id, Phase phase, Integer seasons, Instant startedAt, Instant finishedAt,
                                    TrainingReport report, String error) {
    }

    private static final Duration SYNCHRONOUS_TIMEOUT = Duration.ofMinutes(10);

    private final ObjectProvider<GameOutcomeModel> model;
    private final JobService jobs;
    private final ApplicationEventPublisher events;

    ModelTrainingJobs(ObjectProvider<GameOutcomeModel> model, JobService jobs, ApplicationEventPublisher events) {
        this.model = model;
        this.jobs = jobs;
        this.events = events;
    }

    public TrainingJobStatus start(Integer seasons) {
        GameOutcomeModel engine = engine();
        CurrentUser requestedBy = CurrentUser.get().orElse(null);
        Map<String, String> parameters = new HashMap<>();
        if (seasons != null) {
            parameters.put("seasons", String.valueOf(seasons));
        }
        Job<TrainingReport> job = jobs.start(JobType.MODEL_TRAINING,
                requestedBy == null ? null : new Job.Requester(requestedBy.id(), requestedBy.username()),
                parameters, Phase.TRAINING.name(), context -> train(engine, seasons, requestedBy));
        return toStatus(job);
    }

    public Optional<TrainingJobStatus> latest() {
        return jobs.latest(JobType.MODEL_TRAINING, TrainingReport.class).map(ModelTrainingJobs::toStatus);
    }

    /**
     * Ancien comportement de {@code POST /train/game-outcome} : lance le job et attend sa fin. Gardé tant que
     * l'interface n'a pas basculé sur le suivi du job ; il passe par le même verrou, la même trace et le même audit.
     */
    public TrainingReport trainAndWait(Integer seasons) {
        UUID id = start(seasons).id();
        Instant deadline = Instant.now().plus(SYNCHRONOUS_TIMEOUT);
        while (Instant.now().isBefore(deadline)) {
            TrainingJobStatus status = jobs.find(id, TrainingReport.class).map(ModelTrainingJobs::toStatus).orElseThrow();
            switch (status.phase()) {
                case SUCCEEDED -> {
                    return status.report();
                }
                case FAILED, INTERRUPTED -> throw new AppException(status.error(), HttpStatus.UNPROCESSABLE_CONTENT,
                        "MODEL_TRAINING_FAILED");
                case TRAINING -> sleep();
            }
        }
        throw new AppException("L'entraînement continue en arrière-plan : suivez-le sur la page du modèle.",
                HttpStatus.SERVICE_UNAVAILABLE, "MODEL_TRAINING_RUNNING");
    }

    private TrainingReport train(GameOutcomeModel engine, Integer seasons, CurrentUser requestedBy) {
        try {
            TrainingReport report = engine.train(seasons);
            events.publishEvent(new ModelTrainingEvent.ModelTrained(requestedBy, report.version(), report.accuracy(),
                    report.testSeason()));
            return report;
        } catch (RuntimeException e) {
            events.publishEvent(new ModelTrainingEvent.ModelTrainingFailed(requestedBy, JobService.userMessage(e)));
            throw e;
        }
    }

    private GameOutcomeModel engine() {
        GameOutcomeModel available = model.getIfAvailable();
        if (available == null) {
            throw new AppException("Le moteur de prédiction est désactivé (SPARK_ENABLED=false).",
                    HttpStatus.SERVICE_UNAVAILABLE, "MODEL_ENGINE_DISABLED");
        }
        return available;
    }

    private static TrainingJobStatus toStatus(Job<TrainingReport> job) {
        String requested = job.parameters().get("seasons");
        Integer seasons = requested == null ? null : Integer.valueOf(requested);
        return switch (job.state()) {
            case JobState.Running<TrainingReport> running ->
                    new TrainingJobStatus(job.id(), Phase.TRAINING, seasons, job.createdAt(), null, null, null);
            case JobState.Succeeded<TrainingReport> done ->
                    new TrainingJobStatus(job.id(), Phase.SUCCEEDED, seasons, job.createdAt(), done.finishedAt(),
                            done.result(), null);
            case JobState.Failed<TrainingReport> failed ->
                    new TrainingJobStatus(job.id(), Phase.FAILED, seasons, job.createdAt(), failed.finishedAt(), null,
                            failed.error());
            case JobState.Interrupted<TrainingReport> interrupted ->
                    new TrainingJobStatus(job.id(), Phase.INTERRUPTED, seasons, job.createdAt(),
                            interrupted.detectedAt(), null, "Le serveur s'est arrêté pendant l'entraînement : relancez-le.");
        };
    }

    private static void sleep() {
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException("Attente interrompue.", HttpStatus.SERVICE_UNAVAILABLE, "MODEL_TRAINING_RUNNING");
        }
    }
}
