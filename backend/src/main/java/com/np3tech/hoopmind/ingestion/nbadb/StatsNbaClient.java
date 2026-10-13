package com.np3tech.hoopmind.ingestion.nbadb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Lit le journal des matchs d'une saison sur stats.nba.com ({@code leaguegamelog}) : une ligne par équipe et par
 * match joué, avec la feuille de match de l'équipe. C'est la source d'où Wyatt Walsh tire sa base : mêmes
 * identifiants de match et d'équipe.
 * <p>
 * Le site est public mais refuse les requêtes qui ne ressemblent pas à celles d'un navigateur sur nba.com : les en-têtes
 * ci-dessous l'imitent. Il bloque aussi souvent les adresses d'hébergeurs cloud ; depuis un poste personnel, il répond.
 */
@Component
@Slf4j
public class StatsNbaClient {

    /** Les deux parties de saison importées : saison régulière (identifiants 002…) et playoffs (004…). */
    public enum SeasonType {
        REGULAR_SEASON("Regular Season"), PLAYOFFS("Playoffs");

        private final String parameter;

        SeasonType(String parameter) {
            this.parameter = parameter;
        }
    }

    /** Une ligne du journal : les valeurs de la réponse, par nom de colonne ({@code TEAM_ID}, {@code MATCHUP}, {@code PTS}…). */
    public record GameLogLine(Map<String, Object> values) {

        Object get(String column) {
            return values.get(column);
        }
    }

    private static final Map<String, String> BROWSER_HEADERS = Map.of(
            "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/126.0.0.0 Safari/537.36",
            "Accept", "application/json, text/plain, */*",
            "Accept-Language", "en-US,en;q=0.9",
            "Referer", "https://www.nba.com/",
            "Origin", "https://www.nba.com");

    private final StatsNbaProperties properties;
    private final JsonMapper json;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public StatsNbaClient(StatsNbaProperties properties, JsonMapper json) {
        this.properties = properties;
        this.json = json;
    }

    /**
     * @param season saison au format « 2024-25 »
     */
    @Retryable(includes = StatsNbaUnavailableException.class,
            maxRetriesString = "${hoopmind.import.stats-nba.retry.max-retries:3}",
            delayString = "${hoopmind.import.stats-nba.retry.delay-ms:3000}",
            multiplier = 2, jitter = 500, maxDelay = 30_000)
    public List<GameLogLine> gameLog(String season, SeasonType type) {
        URI uri = URI.create(properties.apiUrl() + "/leaguegamelog?" + query(Map.of(
                "Counter", "0", "Direction", "ASC", "LeagueID", "00", "PlayerOrTeam", "T",
                "Season", season, "SeasonType", type.parameter, "Sorter", "DATE")));
        HttpRequest.Builder request = HttpRequest.newBuilder(uri).GET().timeout(Duration.ofSeconds(60));
        BROWSER_HEADERS.forEach(request::header);
        HttpResponse<byte[]> response;
        try {
            response = http.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            log.warn("stats.nba.com injoignable pour {} {} : {}", season, type, e.toString());
            throw unavailable(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NbaImportException("Lecture de stats.nba.com interrompue.", e);
        }
        int status = response.statusCode();
        if (status == 429 || status >= 500) {
            log.warn("stats.nba.com répond HTTP {} pour {} {}", status, season, type);
            throw unavailable(null);
        }
        if (status != 200) {
            log.warn("stats.nba.com refuse la requête {} {} (HTTP {})", season, type, status);
            throw new NbaImportException("stats.nba.com refuse la requête (HTTP " + status + "). Le site bloque "
                    + "parfois les serveurs hébergés ; lancez l'import depuis un poste personnel.");
        }
        return lines(response.body(), season, type);
    }

    private List<GameLogLine> lines(byte[] body, String season, SeasonType type) {
        Response response;
        try {
            response = json.readValue(body, Response.class);
        } catch (JacksonException e) {
            log.warn("Réponse illisible de stats.nba.com pour {} {}", season, type, e);
            throw new NbaImportException("Réponse illisible de stats.nba.com (le détail est dans le journal du serveur).", e);
        }
        if (response == null || response.resultSets() == null || response.resultSets().isEmpty()
                || response.resultSets().getFirst().headers() == null) {
            log.warn("Réponse sans journal de matchs de stats.nba.com pour {} {}", season, type);
            throw new NbaImportException("Réponse inattendue de stats.nba.com : pas de journal des matchs.");
        }
        ResultSet set = response.resultSets().getFirst();
        List<String> headers = set.headers();
        return set.rowSet() == null ? List.of() : set.rowSet().stream()
                .map(row -> new GameLogLine(IntStream.range(0, Math.min(headers.size(), row.size())).boxed()
                        .filter(i -> row.get(i) != null)
                        .collect(Collectors.toMap(headers::get, row::get, (a, b) -> a))))
                .toList();
    }

    private static StatsNbaUnavailableException unavailable(Throwable cause) {
        return new StatsNbaUnavailableException("stats.nba.com ne répond pas (délai dépassé, réseau ou site surchargé). "
                + "Réessayez plus tard ; le site bloque aussi souvent les serveurs hébergés.", cause);
    }

    private static String query(Map<String, String> parameters) {
        return parameters.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Response(List<ResultSet> resultSets) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ResultSet(String name, List<String> headers, List<List<Object>> rowSet) {
    }
}
