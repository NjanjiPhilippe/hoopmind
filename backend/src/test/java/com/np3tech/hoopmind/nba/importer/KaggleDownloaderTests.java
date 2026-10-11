package com.np3tech.hoopmind.nba.importer;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KaggleDownloaderTests {

    @TempDir
    Path directory;

    private HttpServer server;
    private final List<String> authorizations = new ArrayList<>();
    private int port;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        // Kaggle redirige vers son stockage (un autre hôte), qui sert le fichier compressé en zip.
        server.createContext("/api/v1/datasets/download/wyattowalsh/basketball/nba.sqlite", exchange -> {
            authorizations.add(exchange.getRequestHeaders().getFirst("Authorization"));
            String expected = "Basic " + java.util.Base64.getEncoder().encodeToString("saiko:secret".getBytes());
            if (!expected.equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
                exchange.sendResponseHeaders(401, -1);
                exchange.close();
                return;
            }
            exchange.getResponseHeaders().add("Location", "http://127.0.0.1:" + port + "/storage/nba.zip");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/storage/nba.zip", exchange -> {
            authorizations.add(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = zip("nba.sqlite", "SQLite format 3");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private KaggleDownloader downloader(String username, String key) {
        return new KaggleDownloader(new KaggleProperties(username, key, null, null, "http://localhost:" + port + "/api/v1"));
    }

    @Test
    void downloadsAndUnzipsTheDatabaseWithoutSendingTheKeyToTheStorage() throws IOException {
        Path target = directory.resolve("data/nba.sqlite");
        List<Long> progress = new ArrayList<>();

        downloader("saiko", "secret").download(target, (done, total) -> progress.add(done));

        assertThat(Files.readString(target)).isEqualTo("SQLite format 3");
        assertThat(progress).isNotEmpty();
        assertThat(authorizations).hasSize(2);
        assertThat(authorizations.get(0)).startsWith("Basic ");
        assertThat(authorizations.get(1)).isNull();
        try (var files = Files.list(target.getParent())) {
            assertThat(files).containsExactly(target);
        }
    }

    @Test
    void reportsARejectedKeyAndKeepsTheExistingFile() throws IOException {
        Path target = directory.resolve("nba.sqlite");
        Files.writeString(target, "ancienne base");

        assertThatThrownBy(() -> downloader("saiko", "wrong").download(target, (done, total) -> {
        }))
                .isInstanceOf(NbaImportException.class)
                .hasMessageContaining("KAGGLE_KEY");
        assertThat(Files.readString(target)).isEqualTo("ancienne base");
    }

    @Test
    void refusesToRunWithoutAKey() {
        assertThatThrownBy(() -> downloader("", "").download(directory.resolve("nba.sqlite"), (done, total) -> {
        }))
                .isInstanceOf(NbaImportException.class)
                .hasMessageContaining("KAGGLE_USERNAME");
    }

    private static byte[] zip(String name, String content) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry(name));
            zip.write(content.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }
}
