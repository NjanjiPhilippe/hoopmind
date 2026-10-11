package com.np3tech.hoopmind.ingestion.nbadb;

import com.np3tech.hoopmind.shared.config.ResilienceConfig;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** {@code @Retryable} sur le téléchargement : vérifié à travers le proxy Spring, sans démarrer toute l'application. */
class KaggleRetryTests {

    @TempDir
    Path directory;

    private HttpServer server;
    private final AtomicInteger calls = new AtomicInteger();
    private volatile int failuresBeforeSuccess;
    private volatile int failureStatus;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/datasets/download/wyattowalsh/basketball/nba.sqlite", exchange -> {
            if (calls.incrementAndGet() <= failuresBeforeSuccess) {
                exchange.sendResponseHeaders(failureStatus, -1);
                exchange.close();
                return;
            }
            byte[] body = "SQLite format 3".getBytes(StandardCharsets.UTF_8);
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

    @Test
    void retriesWhenKaggleIsTemporarilyUnavailable() throws IOException {
        failuresBeforeSuccess = 2;
        failureStatus = 503;
        try (var context = context()) {
            Path target = directory.resolve("nba.sqlite");
            context.getBean(KaggleDownloader.class).download(target, (done, total) -> { });

            assertThat(calls).hasValue(3);
            assertThat(Files.readString(target)).isEqualTo("SQLite format 3");
        }
    }

    @Test
    void doesNotRetryARefusedKey() {
        failuresBeforeSuccess = 10;
        failureStatus = 401;
        try (var context = context()) {
            assertThatThrownBy(() -> context.getBean(KaggleDownloader.class)
                    .download(directory.resolve("nba.sqlite"), (done, total) -> { }))
                    .isInstanceOf(NbaImportException.class)
                    .isNotInstanceOf(KaggleUnavailableException.class);
            assertThat(calls).hasValue(1);
        }
    }

    private AnnotationConfigApplicationContext context() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test",
                Map.of("hoopmind.import.kaggle.retry.delay-ms", "10")));
        context.register(ResilienceConfig.class, KaggleDownloader.class);
        context.registerBean(KaggleProperties.class, () -> new KaggleProperties("saiko", "secret", null, null,
                "http://127.0.0.1:" + server.getAddress().getPort() + "/api/v1"));
        context.refresh();
        return context;
    }
}
