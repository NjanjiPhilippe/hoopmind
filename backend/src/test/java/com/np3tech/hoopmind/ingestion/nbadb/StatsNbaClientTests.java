package com.np3tech.hoopmind.ingestion.nbadb;

import com.np3tech.hoopmind.ingestion.nbadb.StatsNbaClient.GameLogLine;
import com.np3tech.hoopmind.ingestion.nbadb.StatsNbaClient.SeasonType;
import com.np3tech.hoopmind.shared.config.ResilienceConfig;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

import static com.np3tech.hoopmind.ingestion.nbadb.StatsNbaFixture.BOS;
import static com.np3tech.hoopmind.ingestion.nbadb.StatsNbaFixture.NYK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Le client stats.nba.com face à un faux serveur, à travers le proxy Spring pour {@code @Retryable}. */
class StatsNbaClientTests {

    private HttpServer server;
    private final AtomicInteger calls = new AtomicInteger();
    private final List<String> queries = new CopyOnWriteArrayList<>();
    private final List<String> referers = new CopyOnWriteArrayList<>();
    private final List<String> origins = new CopyOnWriteArrayList<>();
    private final List<String> protocols = new CopyOnWriteArrayList<>();
    private volatile int failuresBeforeSuccess;
    private volatile int failureStatus;
    private volatile String body = new StatsNbaFixture().played("0022400001", "2024-10-22", BOS, NYK, 132, 109).json();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/stats/leaguegamelog", exchange -> {
            queries.add(exchange.getRequestURI().getRawQuery());
            referers.add(exchange.getRequestHeaders().getFirst("Referer"));
            origins.add(exchange.getRequestHeaders().getFirst("x-nba-stats-origin"));
            protocols.add(exchange.getProtocol());
            if (calls.incrementAndGet() <= failuresBeforeSuccess) {
                exchange.sendResponseHeaders(failureStatus, -1);
                exchange.close();
                return;
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            String encoding = exchange.getRequestHeaders().getFirst("Accept-Encoding");
            if (encoding != null && encoding.contains("gzip")) {
                // Comme stats.nba.com : la réponse est compressée dès que le client l'accepte.
                ByteArrayOutputStream compressed = new ByteArrayOutputStream();
                try (GZIPOutputStream gzip = new GZIPOutputStream(compressed)) {
                    gzip.write(bytes);
                }
                bytes = compressed.toByteArray();
                exchange.getResponseHeaders().add("Content-Encoding", "gzip");
            }
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void readsTheTeamGameLogOfASeasonLikeABrowserOnNbaCom() {
        try (var context = context()) {
            List<GameLogLine> lines = context.getBean(StatsNbaClient.class).gameLog("2024-25", SeasonType.PLAYOFFS);

            assertThat(lines).hasSize(2);
            assertThat(lines.getFirst().get("MATCHUP")).isEqualTo("BOS vs. NYK");
            assertThat(lines.getFirst().get("PTS")).isEqualTo(132);
            assertThat(((Number) lines.getFirst().get("TEAM_ID")).longValue()).isEqualTo(BOS);
            assertThat(queries.getFirst()).contains("Season=2024-25", "SeasonType=Playoffs", "PlayerOrTeam=T",
                    "LeagueID=00");
            assertThat(referers.getFirst()).isEqualTo("https://www.nba.com/");
            assertThat(origins.getFirst()).isEqualTo("stats");
            assertThat(protocols.getFirst()).isEqualTo("HTTP/1.1");
        }
    }

    @Test
    void retriesWhenTheSiteIsTemporarilyUnavailable() {
        failuresBeforeSuccess = 2;
        failureStatus = 503;
        try (var context = context()) {
            assertThat(context.getBean(StatsNbaClient.class).gameLog("2024-25", SeasonType.REGULAR_SEASON)).hasSize(2);
            assertThat(calls).hasValue(3);
        }
    }

    @Test
    void doesNotRetryARefusal() {
        failuresBeforeSuccess = 10;
        failureStatus = 403;
        try (var context = context()) {
            assertThatThrownBy(() -> context.getBean(StatsNbaClient.class).gameLog("2024-25", SeasonType.REGULAR_SEASON))
                    .isInstanceOf(NbaImportException.class)
                    .isNotInstanceOf(StatsNbaUnavailableException.class)
                    .hasMessageContaining("HTTP 403");
            assertThat(calls).hasValue(1);
        }
    }

    @Test
    void anUnexpectedAnswerIsReportedWithoutTechnicalDetails() {
        body = "<html>Access Denied</html>";
        try (var context = context()) {
            assertThatThrownBy(() -> context.getBean(StatsNbaClient.class).gameLog("2024-25", SeasonType.REGULAR_SEASON))
                    .isInstanceOf(NbaImportException.class)
                    .hasMessage("Réponse illisible de stats.nba.com (le détail est dans le journal du serveur).");
        }
    }

    private AnnotationConfigApplicationContext context() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test",
                Map.of("hoopmind.import.stats-nba.retry.delay-ms", "10")));
        context.register(ResilienceConfig.class, StatsNbaClient.class);
        context.registerBean(JsonMapper.class, () -> JsonMapper.builder().build());
        context.registerBean(StatsNbaProperties.class, () -> new StatsNbaProperties(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/stats", 0L, 5L));
        context.refresh();
        return context;
    }
}
