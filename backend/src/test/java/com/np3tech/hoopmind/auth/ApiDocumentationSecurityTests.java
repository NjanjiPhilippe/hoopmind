package com.np3tech.hoopmind.auth;

import com.np3tech.hoopmind.IntegrationTest;
import com.np3tech.hoopmind.TestAccounts;
import com.np3tech.hoopmind.TestAccounts.Session;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class ApiDocumentationSecurityTests {

    @Autowired
    MockMvcTester mvc;

    @Test
    void anonymousVisitorIsAskedForCredentials() {
        assertThat(mvc.get().uri("/v3/api-docs")).hasStatus(401)
                .headers().containsHeader(HttpHeaders.WWW_AUTHENTICATE);
        assertThat(mvc.get().uri("/swagger-ui/index.html")).hasStatus(401);
    }

    @Test
    void regularUserIsRefused() {
        Session user = TestAccounts.register(mvc);

        assertThat(mvc.get().uri("/v3/api-docs").header(HttpHeaders.AUTHORIZATION, basic(user.username(), TestAccounts.PASSWORD)))
                .hasStatus(403);
    }

    @Test
    void adminReadsTheOpenApiDocument() {
        assertThat(mvc.get().uri("/v3/api-docs").header(HttpHeaders.AUTHORIZATION, basic("admin", "AdminPass123")))
                .hasStatusOk()
                .bodyJson().extractingPath("$.paths").asMap().containsKeys("/api/v1/auth/login", "/api/v1/users/me");
    }

    private static String basic(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }
}
