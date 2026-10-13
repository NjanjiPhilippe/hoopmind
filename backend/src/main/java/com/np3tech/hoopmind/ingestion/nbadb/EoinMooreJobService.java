package com.np3tech.hoopmind.ingestion.nbadb;

import com.np3tech.hoopmind.auth.CurrentUser;
import com.np3tech.hoopmind.ingestion.DataImportEvent;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseImporter.ImportReport;
import com.np3tech.hoopmind.nba.repository.SeasonRepository;
import com.np3tech.hoopmind.shared.job.Job;
import com.np3tech.hoopmind.shared.job.JobContext;
import com.np3tech.hoopmind.shared.job.JobService;
import com.np3tech.hoopmind.shared.job.JobState;
import com.np3tech.hoopmind.shared.job.JobType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Importe en arrière-plan les saisons récentes depuis les fichiers d'Eoin Moore déposés sur le serveur
 * ({@link EoinMooreSource}) : par défaut la saison qui suit la dernière saison en base (2023-24 après la base Walsh).
 * Le job prend le même verrou que l'import de la base Walsh.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EoinMooreJobService {

    /** Première saison NBA (1946-47). */
    static final int FIRST_SEASON = 1946;

    /** Les fichiers vont jusqu'à la saison en cours ; au-delà de dix saisons, la base Walsh est le meilleur choix. */
    static final int MAX_SEASONS = 10;

    public enum EoinMoorePhase { READING, IMPORTING, SUCCEEDED, FAILED, INTERRUPTED }

    public record EoinMooreJobStatus(UUID id, EoinMoorePhase phase, String fromSeason, String toSeason,
                                     Instant startedAt, Instant finishedAt, ImportReport report, String error) {
    }

    /** Un des deux fichiers attendus dans le dossier du serveur. */
    public record EoinMooreFile(String name, boolean present, Long sizeBytes, Instant modifiedAt) {
    }

    /** Ce que la page « Données » affiche : le dossier, ses deux fichiers, la saison proposée et le dernier import. */
    public record EoinMooreStatus(String directory, EoinMooreFile games, EoinMooreFile teamStatistics,
                                  int defaultFromYear, int maxSeasons, EoinMooreJobStatus job) {
    }

    private final NbaDatabaseImporter importer;
    private final EoinMooreProperties properties;
    private final SeasonRepository seasons;
    private final JobService jobs;
    private final ApplicationEventPublisher events;

    public EoinMooreStatus status() {
        EoinMooreJobStatus job = jobs.latest(JobType.EOIN_MOORE_IMPORT, ImportReport.class)
                .map(EoinMooreJobService::toStatus).orElse(null);
        return new EoinMooreStatus(properties.directory(), file(EoinMooreSource.GAMES_FILE),
                file(EoinMooreSource.TEAM_STATISTICS_FILE), defaultFrom(), MAX_SEASONS, job);
    }

    /**
     * @param fromYear année de début de la première saison (2023 pour 2023-24) ; par défaut celle qui suit la dernière
     *                 saison en base
     * @param toYear   année de début de la dernière saison ; par défaut {@code fromYear}
     */
    public EoinMooreJobStatus start(Integer fromYear, Integer toYear) {
        int from = fromYear != null ? fromYear : defaultFrom();
        int to = toYear != null ? toYear : from;
        if (from < FIRST_SEASON || to < from || to - from + 1 > MAX_SEASONS) {
            throw new NbaImportException("Choisissez de 1 à " + MAX_SEASONS + " saisons, la première au plus tôt en "
                    + NbaAlignment.season(FIRST_SEASON) + ".");
        }
        if (!file(EoinMooreSource.GAMES_FILE).present() || !file(EoinMooreSource.TEAM_STATISTICS_FILE).present()) {
            throw new NbaImportException("Déposez " + EoinMooreSource.GAMES_FILE + " et "
                    + EoinMooreSource.TEAM_STATISTICS_FILE + " dans le dossier " + properties.directory()
                    + " du serveur (réglage EOINMOORE_DATA_DIR).");
        }
        CurrentUser requestedBy = CurrentUser.get().orElse(null);
        Job<ImportReport> job = jobs.start(JobType.EOIN_MOORE_IMPORT,
                requestedBy == null ? null : new Job.Requester(requestedBy.id(), requestedBy.username()),
                Map.of("from", NbaAlignment.season(from), "to", NbaAlignment.season(to)),
                EoinMoorePhase.READING.name(),
                context -> run(context, from, to, requestedBy));
        return toStatus(job);
    }

    private ImportReport run(JobContext context, int from, int to, CurrentUser requestedBy) {
        String fromSeason = NbaAlignment.season(from);
        String toSeason = NbaAlignment.season(to);
        try {
            ImportReport report;
            try (EoinMooreSource source = EoinMooreSource.read(Path.of(properties.directory()), from, to)) {
                if (source.recentSeasons(MAX_SEASONS).isEmpty()) {
                    throw new NbaImportException("Les fichiers ne contiennent aucun match de saison régulière de "
                            + fromSeason + " à " + toSeason + ".");
                }
                context.step(EoinMoorePhase.IMPORTING.name());
                report = importer.importSource(source, MAX_SEASONS);
            }
            events.publishEvent(new DataImportEvent.EoinMooreImported(requestedBy, report.seasons(), report.games()));
            return report;
        } catch (RuntimeException e) {
            events.publishEvent(new DataImportEvent.EoinMooreImportFailed(requestedBy, fromSeason, toSeason,
                    JobService.userMessage(e)));
            throw e;
        }
    }

    /** La saison qui suit la dernière en base ; 2023-24 si la base est vide (la base Walsh s'arrête à 2022-23). */
    int defaultFrom() {
        return seasons.findAllOrderByYearDesc().stream().findFirst()
                .map(season -> season.getYear() + 1)
                .orElse(2023);
    }

    private EoinMooreFile file(String name) {
        Path path = Path.of(properties.directory()).resolve(name);
        try {
            if (Files.isRegularFile(path)) {
                return new EoinMooreFile(name, true, Files.size(path), Files.getLastModifiedTime(path).toInstant());
            }
        } catch (IOException e) {
            log.warn("Fichier {} illisible : {}", path, e.getMessage());
        }
        return new EoinMooreFile(name, false, null, null);
    }

    private static EoinMooreJobStatus toStatus(Job<ImportReport> job) {
        String from = job.parameters().get("from");
        String to = job.parameters().get("to");
        return switch (job.state()) {
            case JobState.Running<ImportReport> running -> new EoinMooreJobStatus(job.id(),
                    EoinMoorePhase.valueOf(running.step()), from, to, job.createdAt(), null, null, null);
            case JobState.Succeeded<ImportReport> done -> new EoinMooreJobStatus(job.id(), EoinMoorePhase.SUCCEEDED,
                    from, to, job.createdAt(), done.finishedAt(), done.result(), null);
            case JobState.Failed<ImportReport> failed -> new EoinMooreJobStatus(job.id(), EoinMoorePhase.FAILED, from,
                    to, job.createdAt(), failed.finishedAt(), null, failed.error());
            case JobState.Interrupted<ImportReport> interrupted -> new EoinMooreJobStatus(job.id(),
                    EoinMoorePhase.INTERRUPTED, from, to, job.createdAt(), interrupted.detectedAt(), null,
                    "Le serveur s'est arrêté pendant l'import : relancez-le.");
        };
    }
}
