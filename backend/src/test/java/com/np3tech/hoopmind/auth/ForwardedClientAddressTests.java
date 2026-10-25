package com.np3tech.hoopmind.auth;

import com.np3tech.hoopmind.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Derrière nginx, toutes les requêtes viennent de la même adresse : le frein des connexions doit compter par
 * adresse du client, lue dans {@code X-Forwarded-For}. Il faut un vrai serveur Tomcat (pas MockMvc) : c'est lui qui
 * lit l'en-tête, et seulement pour une requête venue d'une adresse privée (ici 127.0.0.1, comme le conteneur nginx).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ForwardedClientAddressTests {

    private static final String PASSWORD = "Password123";

    @LocalServerPort
    int port;

    RestClient http;
    String username;

    @BeforeEach
    void registerAccount() {
        http = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1/auth")
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> { })
                .build();
        username = "fwd-" + UUID.randomUUID().toString().substring(0, 8);
        HttpStatusCode created = http.post().uri("/register").contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"username":"%s","email":"%s@example.com","password":"%s"}""".formatted(username, username, PASSWORD))
                .retrieve().toBodilessEntity().getStatusCode();
        assertThat(created.value()).isEqualTo(201);
    }

    @Test
    void failuresFromOneClientDoNotLockTheAccountForAnotherClient() {
        for (int i = 0; i < 5; i++) {
            assertThat(login("203.0.113.10", "WrongPass123")).isEqualTo(401);
        }
        assertThat(login("203.0.113.10", PASSWORD)).as("l'attaquant est freiné").isEqualTo(429);

        assertThat(login("198.51.100.20", PASSWORD)).as("le vrai propriétaire, depuis une autre adresse").isEqualTo(200);
    }

    private int login(String clientAddress, String password) {
        return http.post().uri("/login").contentType(MediaType.APPLICATION_JSON)
                .header("X-Forwarded-For", clientAddress)
                .body("{\"login\":\"%s\",\"password\":\"%s\"}".formatted(username, password))
                .retrieve().toBodilessEntity().getStatusCode().value();
    }
}
