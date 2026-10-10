package com.np3tech.hoopmind;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Création de comptes et ouverture de sessions dans les tests d'intégration.
 */
public final class TestAccounts {

    public static final String PASSWORD = "Password123";
    public static final String REFRESH_COOKIE = "hoopmind_refresh";

    private TestAccounts() {
    }

    public record Session(long userId, String username, String accessToken, Cookie refreshCookie) {

        public String bearer() {
            return "Bearer " + accessToken;
        }
    }

    public static Session register(MockMvcTester mvc) {
        String username = "user_" + UUID.randomUUID().toString().substring(0, 8);
        MvcTestResult result = mvc.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","email":"%s@example.com","password":"%s","firstName":"Test"}
                        """.formatted(username, username, PASSWORD))
                .exchange();
        assertThat(result).hasStatus(201);
        return session(result, username);
    }

    public static Session login(MockMvcTester mvc, String login, String password) {
        MvcTestResult result = mvc.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"login":"%s","password":"%s"}
                        """.formatted(login, password))
                .exchange();
        assertThat(result).hasStatusOk();
        return session(result, login);
    }

    public static Session admin(MockMvcTester mvc) {
        return login(mvc, "admin", "AdminPass123");
    }

    public static Session session(MvcTestResult result, String username) {
        String body;
        try {
            body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
        Number userId = JsonPath.read(body, "$.user.id");
        return new Session(userId.longValue(), username, JsonPath.read(body, "$.accessToken"),
                result.getResponse().getCookie(REFRESH_COOKIE));
    }
}
