package com.np3tech.hoopmind.auth;

import com.np3tech.hoopmind.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le frontend génère son client depuis {@code frontend/openapi/hoopmind-api.json}. Une route ou un DTO changé sans
 * {@code npm run api:update} laisserait le frontend compiler contre l'ancienne API : ce test le fait échouer.
 * L'adresse du serveur ({@code servers}) n'est pas comparée, elle dépend de la machine qui a produit le fichier.
 */
@IntegrationTest
class OpenApiSnapshotTests {

    private static final Path SNAPSHOT = Path.of("..", "frontend", "openapi", "hoopmind-api.json");

    @Autowired
    MockMvcTester mvc;

    @Test
    void theFrontendSnapshotMatchesTheLiveApi() throws Exception {
        MvcTestResult live = mvc.get().uri("/v3/api-docs")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder()
                        .encodeToString("admin:AdminPass123".getBytes(StandardCharsets.UTF_8)))
                .exchange();
        assertThat(live).hasStatusOk();

        JsonMapper json = JsonMapper.builder().build();
        JsonNode actual = withoutServers(json.readTree(live.getResponse().getContentAsString(StandardCharsets.UTF_8)));
        JsonNode expected = withoutServers(json.readTree(Files.readString(SNAPSHOT)));

        assertThat(actual)
                .as("frontend/openapi/hoopmind-api.json n'est plus à jour : lancer npm run api:update (voir AGENTS.md)")
                .isEqualTo(expected);
    }

    private static JsonNode withoutServers(JsonNode spec) {
        ((ObjectNode) spec).remove("servers");
        return spec;
    }
}
