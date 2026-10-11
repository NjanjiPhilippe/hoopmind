package com.np3tech.hoopmind.nba.importer;

import com.np3tech.hoopmind.IntegrationTest;
import com.np3tech.hoopmind.nba.importer.NbaDatabaseJobService.JobStatus;
import com.np3tech.hoopmind.nba.importer.NbaDatabaseJobService.Phase;
import com.np3tech.hoopmind.nba.repository.SeasonRepository;
import com.np3tech.hoopmind.nba.repository.TeamRepository;
import com.np3tech.hoopmind.shared.exception.AppException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static com.np3tech.hoopmind.nba.importer.NbaDatabaseFixture.BOS;
import static com.np3tech.hoopmind.nba.importer.NbaDatabaseFixture.LAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Le bouton « Télécharger depuis Kaggle et importer » : téléchargement puis import, en arrière-plan. */
@IntegrationTest
class NbaDatabaseJobTests {

    @TempDir
    Path directory;

    @Autowired
    TeamRepository teams;

    @Autowired
    SeasonRepository seasons;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void downloadsFromKaggleThenImportsInTheBackground() throws Exception {
        Path published = directory.resolve("kaggle/nba.sqlite");
        Files.createDirectories(published.getParent());
        try (NbaDatabaseFixture nba = new NbaDatabaseFixture(published)) {
            nba.played("0022400101", "2024-10-22", "2024-25", BOS, LAL, 118, 101);
        }
        byte[] body = Files.readAllBytes(published);
        HttpServer kaggle = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        kaggle.createContext("/api/v1/datasets/download/wyattowalsh/basketball/nba.sqlite", exchange -> {
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        kaggle.start();
        try {
            Path target = directory.resolve("data/nba.sqlite");
            NbaDatabaseImportProperties properties = new NbaDatabaseImportProperties(target.toString(), 1, false);
            KaggleProperties credentials = new KaggleProperties("saiko", "secret", null, null,
                    "http://127.0.0.1:" + kaggle.getAddress().getPort() + "/api/v1");
            NbaDatabaseJobService jobs = new NbaDatabaseJobService(
                    new NbaDatabaseImporter(properties, teams, seasons, jdbc), new KaggleDownloader(credentials),
                    properties, credentials);

            assertThat(jobs.status().file().present()).isFalse();
            assertThat(jobs.start(1, true).phase()).isEqualTo(Phase.DOWNLOADING);
            assertThatThrownBy(() -> jobs.start(1, true))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining("déjà en cours");

            JobStatus done = waitForEnd(jobs);
            assertThat(done.phase()).as(done.error()).isEqualTo(Phase.SUCCEEDED);
            assertThat(done.downloadedBytes()).isEqualTo(body.length);
            assertThat(done.report().seasons()).containsExactly("2024-25");
            assertThat(jobs.status().file().present()).isTrue();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM games WHERE external_id = '0022400101'", Integer.class))
                    .isEqualTo(1);
        } finally {
            kaggle.stop(0);
        }
    }

    private static JobStatus waitForEnd(NbaDatabaseJobService jobs) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(30));
        while (Instant.now().isBefore(deadline)) {
            JobStatus job = jobs.status().job();
            if (job.phase() == Phase.SUCCEEDED || job.phase() == Phase.FAILED) {
                return job;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("import toujours en cours");
    }
}
