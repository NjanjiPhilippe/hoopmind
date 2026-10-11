package com.np3tech.hoopmind.ingestion.nbadb;

import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseImporter.ImportReport;
import com.np3tech.hoopmind.shared.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Lance l'import de la base Walsh en arrière-plan, éventuellement précédé de son téléchargement depuis Kaggle :
 * l'opération dure plusieurs minutes, l'interface suit son avancement au lieu d'attendre la réponse.
 * Une seule opération à la fois ; son état est gardé en mémoire jusqu'à la suivante.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NbaDatabaseJobService {

    public enum Phase { DOWNLOADING, IMPORTING, SUCCEEDED, FAILED }

    /** État d'une opération, tel qu'il est montré à l'administrateur. */
    public record JobStatus(Phase phase, boolean download, int seasons, Instant startedAt, Instant finishedAt,
                            Long downloadedBytes, Long totalBytes, ImportReport report, String error) {

        boolean running() {
            return phase == Phase.DOWNLOADING || phase == Phase.IMPORTING;
        }
    }

    /** Le fichier de la base Walsh présent sur le serveur. */
    public record DatabaseFile(String path, boolean present, Long sizeBytes, Instant modifiedAt) {
    }

    /** Ce que la page « Données » affiche pour la base Walsh. */
    public record NbaDatabaseStatus(DatabaseFile file, boolean kaggleConfigured, String kaggleDataset,
                                    int defaultSeasons, int maxSeasons, JobStatus job) {
    }

    private final NbaDatabaseImporter importer;
    private final KaggleDownloader downloader;
    private final NbaDatabaseImportProperties properties;
    private final KaggleProperties kaggle;

    private final AtomicReference<JobStatus> current = new AtomicReference<>();

    public NbaDatabaseStatus status() {
        return new NbaDatabaseStatus(file(), kaggle.configured(), kaggle.dataset(), properties.seasons(),
                NbaDatabaseImporter.MAX_SEASONS, current.get());
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
            throw new NbaImportException("Le fichier " + properties.path() + " est absent : téléchargez d'abord la base.");
        }
        JobStatus started = new JobStatus(download ? Phase.DOWNLOADING : Phase.IMPORTING, download, seasonCount,
                Instant.now(), null, null, null, null, null);
        JobStatus previous = current.get();
        if ((previous != null && previous.running()) || !current.compareAndSet(previous, started)) {
            throw new AppException("Un import est déjà en cours.", HttpStatus.CONFLICT, "IMPORT_ALREADY_RUNNING");
        }
        Thread.ofVirtual().name("nba-database-import").start(() -> run(started));
        return started;
    }

    private void run(JobStatus job) {
        try {
            if (job.download()) {
                downloader.download(Path.of(properties.path()), (done, total) -> current.updateAndGet(s ->
                        new JobStatus(s.phase(), s.download(), s.seasons(), s.startedAt(), null, done,
                                total < 0 ? null : total, null, null)));
                current.updateAndGet(s -> new JobStatus(Phase.IMPORTING, s.download(), s.seasons(), s.startedAt(), null,
                        s.downloadedBytes(), s.totalBytes(), null, null));
            }
            ImportReport report = importer.importDatabase(job.seasons());
            current.updateAndGet(s -> new JobStatus(Phase.SUCCEEDED, s.download(), s.seasons(), s.startedAt(),
                    Instant.now(), s.downloadedBytes(), s.totalBytes(), report, null));
        } catch (RuntimeException e) {
            log.error("Échec de l'import de la base NBA", e);
            String message = e instanceof AppException ? e.getMessage() : "Erreur inattendue : " + e.getMessage();
            current.updateAndGet(s -> new JobStatus(Phase.FAILED, s.download(), s.seasons(), s.startedAt(),
                    Instant.now(), s.downloadedBytes(), s.totalBytes(), null, message));
        }
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
