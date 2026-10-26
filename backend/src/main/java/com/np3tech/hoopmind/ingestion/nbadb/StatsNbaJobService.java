package com.np3tech.hoopmind.ingestion.nbadb;

import com.np3tech.hoopmind.auth.CurrentUser;
import com.np3tech.hoopmind.ingestion.DataImportEvent;
import com.np3tech.hoopmind.ingestion.nbadb.NbaDatabaseImporter.ImportReport;
import com.np3tech.hoopmind.ingestion.nbadb.StatsNbaClient.GameLogLine;
import com.np3tech.hoopmind.ingestion.nbadb.StatsNbaClient.SeasonType;
import com.np3tech.hoopmind.nba.entity.Season;
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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Complète la base avec les saisons que la base de Wyatt Walsh n'a pas encore, lues sur stats.nba.com, en
 * arrière-plan : de la dernière saison en base (relue pour la compléter) jusqu'à la saison en cours.
 * <p>
 * Toutes les saisons sont lues avant d'écrire quoi que ce soit : une coupure du site en cours de route ne laisse
 * pas d'import partiel. Le job prend le même verrou que l'import de la base Walsh.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StatsNbaJobService {

    /** Au-delà, l'import demanderait des dizaines de requêtes : la base Walsh couvre déjà l'historique. */
    static final int MAX_SEASONS = 30;

    public enum StatsNbaPhase { FETCHING, IMPORTING, SUCCEEDED, FAILED, INTERRUPTED }

    /**
     * État d'un import depuis stats.nba.com, tel qu'il est montré à l'administrateur.
     *
     * @param fetched requêtes faites sur stats.nba.com (deux par saison : saison régulière et playoffs)
     */
    public record StatsNbaJobStatus(UUID id, StatsNbaPhase phase, String fromSeason, String toSeason, Instant startedAt,
                                    Instant finishedAt, Long fetched, Long requests, ImportReport report, String error) {
    }

    /** Ce que la page « Données » affiche : les saisons qu'un import lirait, et le dernier import. */
    public record StatsNbaStatus(String fromSeason, String toSeason, StatsNbaJobStatus job) {
    }

    private final StatsNbaClient client;
    private final StatsNbaProperties properties;
    private final NbaDatabaseImporter importer;
    private final NbaDatabaseImportProperties importProperties;
    private final SeasonRepository seasons;
    private final JobService jobs;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public StatsNbaStatus status() {
        int to = currentSeason();
        StatsNbaJobStatus job = jobs.latest(JobType.STATS_NBA_IMPORT, ImportReport.class)
                .map(StatsNbaJobService::toStatus).orElse(null);
        return new StatsNbaStatus(NbaAlignment.season(defaultFrom(to)), NbaAlignment.season(to), job);
    }

    /**
     * @param fromYear année de début de la première saison à lire (2023 pour 2023-24) ; par défaut la dernière saison
     *                 en base, ou les {@code hoopmind.import.nba-database.seasons} dernières saisons si la base est vide
     */
    public StatsNbaJobStatus start(Integer fromYear) {
        int to = currentSeason();
        int from = fromYear != null ? fromYear : defaultFrom(to);
        if (from > to || to - from + 1 > MAX_SEASONS) {
            throw new NbaImportException("La première saison doit être comprise entre "
                    + NbaAlignment.season(to - MAX_SEASONS + 1) + " et " + NbaAlignment.season(to) + ".");
        }
        CurrentUser requestedBy = CurrentUser.get().orElse(null);
        Job<ImportReport> job = jobs.start(JobType.STATS_NBA_IMPORT,
                requestedBy == null ? null : new Job.Requester(requestedBy.id(), requestedBy.username()),
                Map.of("from", NbaAlignment.season(from), "to", NbaAlignment.season(to)),
                StatsNbaPhase.FETCHING.name(),
                context -> run(context, from, to, requestedBy));
        return toStatus(job);
    }

    private ImportReport run(JobContext context, int from, int to, CurrentUser requestedBy) {
        String fromSeason = NbaAlignment.season(from);
        String toSeason = NbaAlignment.season(to);
        try {
            Map<String, List<GameLogLine>> lines = new LinkedHashMap<>();
            long requests = 2L * (to - from + 1);
            long done = 0;
            context.progress(done, requests);
            String unavailableSeason = null;
            StatsNbaUnavailableException unavailable = null;
            fetch:
            for (int year = from; year <= to; year++) {
                String season = NbaAlignment.season(year);
                List<GameLogLine> seasonLines = new ArrayList<>();
                for (SeasonType type : SeasonType.values()) {
                    if (done > 0) {
                        pause();
                    }
                    try {
                        seasonLines.addAll(client.gameLog(season, type));
                    } catch (StatsNbaUnavailableException e) {
                        // Le site ralentit puis cesse de répondre après quelques requêtes : les saisons déjà lues
                        // sont importées, et l'import suivant reprend à partir de la dernière saison en base.
                        unavailableSeason = season;
                        unavailable = e;
                        break fetch;
                    }
                    context.progress(++done, requests);
                }
                lines.put(season, seasonLines);
                log.info("stats.nba.com, saison {} : {} lignes", season, seasonLines.size());
            }
            if (lines.isEmpty() && unavailable != null) {
                throw unavailable;
            }
            context.step(StatsNbaPhase.IMPORTING.name());
            ImportReport report;
            try (StatsNbaSource source = new StatsNbaSource(lines)) {
                if (source.recentSeasons(MAX_SEASONS).isEmpty()) {
                    throw new NbaImportException("stats.nba.com n'a renvoyé aucun match de saison régulière de "
                            + fromSeason + " à " + toSeason + ".");
                }
                report = importer.importSource(source, MAX_SEASONS);
            }
            events.publishEvent(new DataImportEvent.StatsNbaImported(requestedBy, report.seasons(), report.games()));
            if (unavailable != null) {
                throw new NbaImportException("Import partiel : " + String.join(", ", report.seasons())
                        + " importée(s) (" + report.games() + " matchs), puis stats.nba.com a cessé de répondre pour "
                        + unavailableSeason + ". Relancez dans quelques minutes : l'import reprendra là où il s'est arrêté.",
                        unavailable);
            }
            return report;
        } catch (RuntimeException e) {
            events.publishEvent(new DataImportEvent.StatsNbaImportFailed(requestedBy, fromSeason, toSeason,
                    JobService.userMessage(e)));
            throw e;
        }
    }

    private void pause() {
        try {
            Thread.sleep(properties.delayMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NbaImportException("Lecture de stats.nba.com interrompue.", e);
        }
    }

    /** La saison NBA commence en octobre : en septembre 2026, la saison en cours est encore 2025-26. */
    int currentSeason() {
        LocalDate today = LocalDate.now(clock);
        return today.getMonthValue() >= Month.OCTOBER.getValue() ? today.getYear() : today.getYear() - 1;
    }

    private int defaultFrom(int to) {
        return seasons.findAllByOrderByYearDesc().stream().findFirst()
                .map(Season::getYear)
                .filter(year -> year <= to && to - year + 1 <= MAX_SEASONS)
                .orElse(to - Math.min(importProperties.seasons(), MAX_SEASONS) + 1);
    }

    private static StatsNbaJobStatus toStatus(Job<ImportReport> job) {
        String from = job.parameters().get("from");
        String to = job.parameters().get("to");
        return switch (job.state()) {
            case JobState.Running<ImportReport> running -> new StatsNbaJobStatus(job.id(),
                    StatsNbaPhase.valueOf(running.step()), from, to, job.createdAt(), null, running.done(),
                    running.total(), null, null);
            case JobState.Succeeded<ImportReport> done -> new StatsNbaJobStatus(job.id(), StatsNbaPhase.SUCCEEDED, from,
                    to, job.createdAt(), done.finishedAt(), null, null, done.result(), null);
            case JobState.Failed<ImportReport> failed -> new StatsNbaJobStatus(job.id(), StatsNbaPhase.FAILED, from, to,
                    job.createdAt(), failed.finishedAt(), null, null, null, failed.error());
            case JobState.Interrupted<ImportReport> interrupted -> new StatsNbaJobStatus(job.id(),
                    StatsNbaPhase.INTERRUPTED, from, to, job.createdAt(), interrupted.detectedAt(), null, null, null,
                    "Le serveur s'est arrêté pendant l'import : relancez-le.");
        };
    }
}
