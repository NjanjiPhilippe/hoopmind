package com.np3tech.hoopmind.ingestion.nbadb;

import com.np3tech.hoopmind.auth.CurrentUser;
import com.np3tech.hoopmind.ingestion.DataImportEvent;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseImporter.ImportReport;
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
 * Lance l'import de la base Walsh en arrière-plan, éventuellement précédé de son téléchargement depuis Kaggle :
 * l'opération dure plusieurs minutes, l'interface suit son avancement au lieu d'attendre la réponse.
 * Le job est persisté ({@link JobService}) et prend le verrou des données : une seule opération à la fois.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NbaDatabaseJobService {

    public enum Phase { DOWNLOADING, IMPORTING, SUCCEEDED, FAILED, INTERRUPTED }

    /**
     * État d'une opération, tel qu'il est montré à l'administrateur.
     *
     * @param downloadedBytes octets téléchargés (pendant et après le téléchargement)
     */
    public record JobStatus(UUID id, Phase phase, boolean download, int seasons, Instant startedAt, Instant finishedAt,
                            Long downloadedBytes, Long totalBytes, ImportReport report, String error) {
    }

    /** Le fichier de la base Walsh présent sur le serveur. */
    public record DatabaseFile(String path, boolean present, Long sizeBytes, Instant modifiedAt) {
    }

    /** Ce que la page « Données » affiche pour la base Walsh. */
    public record NbaDatabaseStatus(DatabaseFile file, boolean kaggleConfigured, String kaggleDataset,
                                    int defaultSeasons, int maxSeasons, JobStatus job) {
    }

    /** Résultat enregistré d'un import réussi. */
    record ImportOutcome(Long downloadedBytes, ImportReport report) {
    }

    private final NbaDatabaseImporter importer;
    private final KaggleDownloader downloader;
    private final NbaDatabaseImportProperties properties;
    private final KaggleProperties kaggle;
    private final JobService jobs;
    private final ApplicationEventPublisher events;

    public NbaDatabaseStatus status() {
        JobStatus job = jobs.latest(JobType.NBA_DATABASE_IMPORT, ImportOutcome.class).map(NbaDatabaseJobService::toStatus)
                .orElse(null);
        return new NbaDatabaseStatus(file(), kaggle.configured(), kaggle.dataset(), properties.seasons(),
                NbaDatabaseImporter.MAX_SEASONS, job);
    }

    /**
     * @param seasons  nombre de saisons à importer ; {@code null} reprend le réglage par défaut
     * @param download télécharger d'abord la base depuis Kaggle (sinon : importer le fichier déjà présent)
     */
    public JobStatus start(Integer seasons, boolean download) {
        int seasonCount = seasons != null ? seasons : properties.seasons();
        if (seasonCount < 1 || seasonCount > NbaDatabaseImporter.MAX_SEASONS) {
            throw new NbaImportException("Le nombre de saisons doit être compris entre 1 et "
                    + NbaDatabaseImporter.MAX_SEASONS + ".");
        }
        if (download && !kaggle.configured()) {
            throw new NbaImportException("Aucune clé Kaggle configurée sur le serveur (KAGGLE_USERNAME et KAGGLE_KEY).");
        }
        if (!download && !file().present()) {
            throw new NbaImportException("La base NBA est absente du serveur : téléchargez-la d'abord.");
        }
        // Le job tourne hors requête : l'administrateur qui l'a lancé est retenu maintenant pour le journal d'audit.
        CurrentUser requestedBy = CurrentUser.get().orElse(null);
        Job<ImportOutcome> job = jobs.start(JobType.NBA_DATABASE_IMPORT,
                requestedBy == null ? null : new Job.Requester(requestedBy.id(), requestedBy.username()),
                Map.of("seasons", String.valueOf(seasonCount), "download", String.valueOf(download)),
                (download ? Phase.DOWNLOADING : Phase.IMPORTING).name(),
                context -> run(context, seasonCount, download, requestedBy));
        return toStatus(job);
    }

    private ImportOutcome run(JobContext context, int seasons, boolean download, CurrentUser requestedBy) {
        try {
            Long downloaded = null;
            if (download) {
                long[] bytes = {0};
                downloader.download(Path.of(properties.path()), (done, total) -> {
                    bytes[0] = done;
                    context.progress(done, total < 0 ? null : total);
                });
                downloaded = bytes[0];
                context.step(Phase.IMPORTING.name());
            }
            ImportReport report = importer.importDatabase(seasons);
            events.publishEvent(new DataImportEvent.NbaDatabaseImported(requestedBy, download, report.seasons(),
                    report.games()));
            return new ImportOutcome(downloaded, report);
        } catch (RuntimeException e) {
            events.publishEvent(new DataImportEvent.NbaDatabaseImportFailed(requestedBy, download, seasons,
                    JobService.userMessage(e)));
            throw e;
        }
    }

    private static JobStatus toStatus(Job<ImportOutcome> job) {
        boolean download = Boolean.parseBoolean(job.parameters().get("download"));
        int seasons = Integer.parseInt(job.parameters().getOrDefault("seasons", "0"));
        return switch (job.state()) {
            case JobState.Running<ImportOutcome> running -> new JobStatus(job.id(), Phase.valueOf(running.step()),
                    download, seasons, job.createdAt(), null, running.done(), running.total(), null, null);
            case JobState.Succeeded<ImportOutcome> done -> new JobStatus(job.id(), Phase.SUCCEEDED, download, seasons,
                    job.createdAt(), done.finishedAt(), done.result().downloadedBytes(), done.result().downloadedBytes(),
                    done.result().report(), null);
            case JobState.Failed<ImportOutcome> failed -> new JobStatus(job.id(), Phase.FAILED, download, seasons,
                    job.createdAt(), failed.finishedAt(), null, null, null, failed.error());
            case JobState.Interrupted<ImportOutcome> interrupted -> new JobStatus(job.id(), Phase.INTERRUPTED, download,
                    seasons, job.createdAt(), interrupted.detectedAt(), null, null, null,
                    "Le serveur s'est arrêté pendant l'import : relancez-le.");
        };
    }

    private DatabaseFile file() {
        Path path = Path.of(properties.path());
        try {
            if (Files.isRegularFile(path)) {
                return new DatabaseFile(properties.path(), true, Files.size(path),
                        Files.getLastModifiedTime(path).toInstant());
            }
        } catch (IOException e) {
            log.warn("Fichier {} illisible : {}", path, e.getMessage());
        }
        return new DatabaseFile(properties.path(), false, null, null);
    }
}
