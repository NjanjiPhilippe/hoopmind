package com.np3tech.hoopmind.user;

import com.np3tech.hoopmind.IntegrationTest;
import com.np3tech.hoopmind.TestAccounts;
import com.np3tech.hoopmind.TestAccounts.Session;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class UserManagementTests {

    @Autowired
    MockMvcTester mvc;

    @Test
    void userReadsAndUpdatesOwnProfile() {
        Session session = TestAccounts.register(mvc);

        assertThat(mvc.patch().uri("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, session.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lastName\":\"Curry\",\"bio\":\"Fan des Warriors\",\"favoriteTeamId\":10}"))
                .hasStatusOk();

        assertThat(mvc.get().uri("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.lastName", v -> assertThat(v).isEqualTo("Curry"))
                .hasPathSatisfying("$.favoriteTeamId", v -> assertThat(v).asNumber().isEqualTo(10))
                .hasPathSatisfying("$.roles", v -> assertThat(v).asArray().containsExactly("ROLE_USER"))
                .doesNotHavePath("$.password")
                .doesNotHavePath("$.passwordHash");
    }

    @Test
    void regularUserCannotReachAdministration() {
        Session session = TestAccounts.register(mvc);

        assertThat(mvc.get().uri("/api/v1/admin/users").header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .hasStatus(403)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void adminSearchesUsers() {
        Session user = TestAccounts.register(mvc);
        Session admin = TestAccounts.admin(mvc);

        assertThat(mvc.get().uri("/api/v1/admin/users").param("search", user.username())
                .header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.content[0].username", v -> assertThat(v).isEqualTo(user.username()))
                .hasPathSatisfying("$.page.totalElements", v -> assertThat(v).asNumber().isEqualTo(1));
    }

    @Test
    void disablingAUserBlocksLoginAndRevokesSessions() {
        Session user = TestAccounts.register(mvc);
        Session admin = TestAccounts.admin(mvc);

        assertThat(mvc.put().uri("/api/v1/admin/users/{id}/status", user.userId())
                .header(HttpHeaders.AUTHORIZATION, admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .hasStatusOk();

        assertThat(mvc.post().uri("/api/v1/auth/refresh").cookie(user.refreshCookie())).hasStatus(401);
        assertThat(mvc.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"%s\",\"password\":\"Password123\"}".formatted(user.username())))
                .hasStatus(401);
    }

    @Test
    void promotedUserGetsAdminRoleOnNextLogin() {
        Session user = TestAccounts.register(mvc);
        Session admin = TestAccounts.admin(mvc);

        assertThat(mvc.put().uri("/api/v1/admin/users/{id}/roles", user.userId())
                .header(HttpHeaders.AUTHORIZATION, admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"ROLE_ADMIN\"]}"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.roles").asArray().containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");

        Session promoted = TestAccounts.login(mvc, user.username(), TestAccounts.PASSWORD);
        assertThat(mvc.get().uri("/api/v1/admin/users").header(HttpHeaders.AUTHORIZATION, promoted.bearer()))
                .hasStatusOk();
    }

    @Test
    void adminDeletesAUser() {
        Session user = TestAccounts.register(mvc);
        Session admin = TestAccounts.admin(mvc);

        assertThat(mvc.delete().uri("/api/v1/admin/users/{id}", user.userId())
                .header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .hasStatus(204);
        assertThat(mvc.get().uri("/api/v1/admin/users/{id}", user.userId())
                .header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .hasStatus(404);
    }
}
