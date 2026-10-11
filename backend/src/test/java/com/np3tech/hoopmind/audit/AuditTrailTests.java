package com.np3tech.hoopmind.audit;

import com.np3tech.hoopmind.IntegrationTest;
import com.np3tech.hoopmind.TestAccounts;
import com.np3tech.hoopmind.TestAccounts.Session;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class AuditTrailTests {

    @Autowired
    MockMvcTester mvc;

    @Test
    void recordsWhatAUserDidToTheirOwnAccount() {
        Session user = TestAccounts.register(mvc);
        TestAccounts.login(mvc, user.username(), TestAccounts.PASSWORD);
        mvc.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"%s\",\"password\":\"mauvais-mot-de-passe\"}".formatted(user.username()))
                .exchange();
        assertThat(mvc.patch().uri("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, user.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"bio\":\"Fan des Celtics\"}"))
                .hasStatusOk();
        assertThat(mvc.put().uri("/api/v1/users/me/password").header(HttpHeaders.AUTHORIZATION, user.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"faux\",\"newPassword\":\"NouveauPass123\"}"))
                .hasStatus(400);

        List<Map<String, Object>> events = eventsFor(user.userId());

        assertThat(events).extracting(e -> e.get("action") + "/" + e.get("outcome")).containsExactly(
                "PASSWORD_CHANGE_FAILED/FAILURE",
                "PROFILE_UPDATED/SUCCESS",
                "LOGIN_FAILED/FAILURE",
                "LOGIN_SUCCEEDED/SUCCESS",
                "USER_REGISTERED/SUCCESS");
        assertThat(events).allSatisfy(e -> {
            assertThat(e.get("actorLogin")).isEqualTo(user.username());
            assertThat(e.get("targetLabel")).isEqualTo(user.username());
        });
        assertThat(events.get(1).get("details")).isEqualTo(Map.of("fields", "bio"));
        assertThat(events.get(2).get("details")).isEqualTo(Map.of("reason", "BAD_PASSWORD"));
    }

    @Test
    void recordsAdminActionsWithTheAdminAsAuthor() {
        Session user = TestAccounts.register(mvc);
        Session admin = TestAccounts.admin(mvc);

        assertThat(mvc.put().uri("/api/v1/admin/users/{id}/roles", user.userId())
                .header(HttpHeaders.AUTHORIZATION, admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"ROLE_ADMIN\"]}"))
                .hasStatusOk();
        assertThat(mvc.put().uri("/api/v1/admin/users/{id}/status", user.userId())
                .header(HttpHeaders.AUTHORIZATION, admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .hasStatusOk();
        assertThat(mvc.delete().uri("/api/v1/admin/users/{id}", user.userId())
                .header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .hasStatus(204);

        List<Map<String, Object>> events = eventsFor(user.userId());

        assertThat(events.subList(0, 3)).extracting(e -> e.get("action")).containsExactly(
                "USER_DELETED", "USER_DISABLED", "USER_ROLES_CHANGED");
        assertThat(events.subList(0, 3)).allSatisfy(e -> {
            assertThat(e.get("actorLogin")).isEqualTo("admin");
            assertThat(e.get("targetLabel")).isEqualTo(user.username());
        });
        assertThat(events.get(2).get("details"))
                .isEqualTo(Map.of("before", "ROLE_USER", "after", "ROLE_ADMIN,ROLE_USER"));
    }

    @Test
    void savingTheSameRolesIsNotAChange() {
        Session user = TestAccounts.register(mvc);
        Session admin = TestAccounts.admin(mvc);

        assertThat(mvc.put().uri("/api/v1/admin/users/{id}/roles", user.userId())
                .header(HttpHeaders.AUTHORIZATION, admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"ROLE_USER\"]}"))
                .hasStatusOk();

        assertThat(eventsFor(user.userId())).extracting(e -> e.get("action")).doesNotContain("USER_ROLES_CHANGED");
    }

    @Test
    void unknownLoginIsRecordedWithoutTheTypedIdentifier() {
        String typed = "inconnu-" + System.nanoTime();
        assertThat(mvc.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"%s\",\"password\":\"x\"}".formatted(typed)))
                .hasStatus(401);

        Session admin = TestAccounts.admin(mvc);
        assertThat(mvc.get().uri("/api/v1/admin/audit-events").param("action", "LOGIN_FAILED")
                .header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.content[?(@.details.reason == 'UNKNOWN_ACCOUNT')]", v -> assertThat(v).asArray().isNotEmpty())
                .doesNotHavePath("$.content[0].actorLogin");

        assertThat(mvc.get().uri("/api/v1/admin/audit-events").param("user", typed)
                .header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.page.totalElements", v -> assertThat(v).asNumber().isEqualTo(0));
    }

    @Test
    void recordsDataImportsWithTheAdminWhoStartedThem() {
        Session admin = TestAccounts.admin(mvc);
        var file = new MockMultipartFile("files", "inconnu.csv", "text/csv",
                "colonne_a,colonne_b\n1,2\n".getBytes(StandardCharsets.UTF_8));
        assertThat(mvc.post().uri("/api/v1/admin/imports/csv").multipart().file(file)
                .header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .hasStatusOk();

        assertThat(mvc.get().uri("/api/v1/admin/audit-events").param("action", "DATA_IMPORTED")
                .header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.content[0].outcome").isEqualTo("SUCCESS");
                    assertThat(json).extractingPath("$.content[0].actorLogin").isEqualTo("admin");
                    assertThat(json).extractingPath("$.content[0].targetLabel").isEqualTo("Archive NBA Stats (CSV)");
                    assertThat(json).extractingPath("$.content[0].details.files").isEqualTo("1");
                    assertThat(json).extractingPath("$.content[0].details.importedFiles").isEqualTo("0");
                });
    }

    @Test
    void onlyAdminsCanReadTheJournal() {
        Session user = TestAccounts.register(mvc);

        assertThat(mvc.get().uri("/api/v1/admin/audit-events").header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .hasStatus(403);
        assertThat(mvc.get().uri("/api/v1/admin/audit-events")).hasStatus(401);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> eventsFor(long userId) {
        Session admin = TestAccounts.admin(mvc);
        var result = mvc.get().uri("/api/v1/admin/audit-events").param("userId", String.valueOf(userId))
                .header(HttpHeaders.AUTHORIZATION, admin.bearer())
                .exchange();
        assertThat(result).hasStatusOk();
        return (List<Map<String, Object>>) assertThat(result).bodyJson().convertTo(Map.class).actual().get("content");
    }
}
