package com.np3tech.hoopmind.auth;

import com.np3tech.hoopmind.IntegrationTest;
import com.np3tech.hoopmind.TestAccounts;
import com.np3tech.hoopmind.TestAccounts.Session;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class AuthenticationFlowTests {

    @Autowired
    MockMvcTester mvc;

    @Test
    void registerReturnsAccessTokenAndHttpOnlyRefreshCookie() {
        Session session = TestAccounts.register(mvc);

        assertThat(session.accessToken()).isNotBlank();
        assertThat(session.refreshCookie()).isNotNull();
        assertThat(session.refreshCookie().isHttpOnly()).isTrue();
        assertThat(session.refreshCookie().getPath()).isEqualTo("/api/v1/auth");
    }

    @Test
    void duplicateUsernameIsRejectedAsConflict() {
        Session existing = TestAccounts.register(mvc);

        assertThat(mvc.post().uri("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","email":"other-%s@example.com","password":"Password123"}
                        """.formatted(existing.username().toUpperCase(), existing.username())))
                .hasStatus(409)
                .bodyJson().extractingPath("$.type").isEqualTo("https://hoopmind.dev/problems/username-taken");
    }

    @Test
    void invalidRegistrationListsFieldErrors() {
        assertThat(mvc.post().uri("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"a","email":"not-an-email","password":"short"}
                        """))
                .hasStatus(400)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().extractingPath("$.errors").asMap().containsKeys("username", "email", "password");
    }

    @Test
    void loginAcceptsEmailCaseInsensitively() {
        Session registered = TestAccounts.register(mvc);

        Session session = TestAccounts.login(mvc, registered.username().toUpperCase() + "@EXAMPLE.COM", TestAccounts.PASSWORD);

        assertThat(session.userId()).isEqualTo(registered.userId());
    }

    @Test
    void wrongPasswordAndUnknownUserGetTheSameAnswer() throws Exception {
        Session registered = TestAccounts.register(mvc);

        String wrongPassword = mvc.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"%s\",\"password\":\"WrongPass123\"}".formatted(registered.username()))
                .exchange().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        MvcTestResult unknownUser = mvc.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"nobody-here\",\"password\":\"WrongPass123\"}").exchange();

        assertThat(unknownUser).hasStatus(401);
        assertThat(wrongPassword).contains("bad-credentials");
        assertThat(unknownUser).bodyJson().extractingPath("$.type").isEqualTo("https://hoopmind.dev/problems/bad-credentials");
    }

    @Test
    void protectedEndpointRequiresToken() {
        assertThat(mvc.get().uri("/api/v1/users/me"))
                .hasStatus(401)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .headers().hasValue(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
    }

    @Test
    void tamperedTokenIsRejected() {
        Session session = TestAccounts.register(mvc);

        assertThat(mvc.get().uri("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, session.bearer() + "x"))
                .hasStatus(401);
    }

    @Test
    void refreshRotatesTheTokenAndDetectsReuse() {
        Session session = TestAccounts.register(mvc);
        Cookie first = session.refreshCookie();

        MvcTestResult refreshed = refresh(first);
        assertThat(refreshed).hasStatusOk();
        Cookie second = refreshed.getResponse().getCookie(TestAccounts.REFRESH_COOKIE);
        assertThat(second.getValue()).isNotEqualTo(first.getValue());

        // L'ancien jeton rejoué : refusé, et toute la famille est révoquée
        assertThat(refresh(first)).hasStatus(401);
        assertThat(refresh(second)).hasStatus(401);
    }

    @Test
    void refreshWithoutCookieIsUnauthorized() {
        assertThat(mvc.post().uri("/api/v1/auth/refresh")).hasStatus(401);
    }

    @Test
    void logoutRevokesTheSessionAndClearsTheCookie() {
        Session session = TestAccounts.register(mvc);

        MvcTestResult logout = mvc.post().uri("/api/v1/auth/logout").cookie(session.refreshCookie()).exchange();

        assertThat(logout).hasStatus(204);
        assertThat(logout.getResponse().getCookie(TestAccounts.REFRESH_COOKIE).getMaxAge()).isZero();
        assertThat(refresh(session.refreshCookie())).hasStatus(401);
    }

    @Test
    void changingPasswordRevokesExistingSessions() {
        Session session = TestAccounts.register(mvc);

        assertThat(mvc.put().uri("/api/v1/users/me/password").header(HttpHeaders.AUTHORIZATION, session.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"Password123\",\"newPassword\":\"NewPassword456\"}"))
                .hasStatus(204);

        assertThat(refresh(session.refreshCookie())).hasStatus(401);
        TestAccounts.login(mvc, session.username(), "NewPassword456");
    }

    @Test
    void changingPasswordRequiresTheCurrentOne() {
        Session session = TestAccounts.register(mvc);

        assertThat(mvc.put().uri("/api/v1/users/me/password").header(HttpHeaders.AUTHORIZATION, session.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"WrongPass123\",\"newPassword\":\"NewPassword456\"}"))
                .hasStatus(400);
    }

    private MvcTestResult refresh(Cookie cookie) {
        return mvc.post().uri("/api/v1/auth/refresh").cookie(cookie).exchange();
    }

    @Test
    void tooManyFailedLoginsBlockTheAddressWithRetryAfterEvenForTheRightPassword() {
        Session user = TestAccounts.register(mvc);
        String wrong = "{\"login\":\"%s\",\"password\":\"mauvais\"}".formatted(user.username());
        String right = "{\"login\":\"%s\",\"password\":\"%s\"}".formatted(user.username(), TestAccounts.PASSWORD);
        for (int i = 0; i < 5; i++) {
            assertThat(mvc.post().uri("/api/v1/auth/login").with(fromAddress("192.0.2.10"))
                    .contentType(MediaType.APPLICATION_JSON).content(wrong)).hasStatus(401);
        }

        assertThat(mvc.post().uri("/api/v1/auth/login").with(fromAddress("192.0.2.10"))
                .contentType(MediaType.APPLICATION_JSON).content(right))
                .hasStatus(429)
                .hasHeader(HttpHeaders.RETRY_AFTER, "30")
                .bodyJson().extractingPath("$.code").isEqualTo("TOO_MANY_ATTEMPTS");

        // Le propriétaire du compte peut toujours se connecter depuis une autre adresse.
        assertThat(mvc.post().uri("/api/v1/auth/login").with(fromAddress("192.0.2.11"))
                .contentType(MediaType.APPLICATION_JSON).content(right))
                .hasStatusOk();
    }

    private static RequestPostProcessor fromAddress(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }
}
