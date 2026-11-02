package com.np3tech.hoopmind.auth;

import com.jayway.jsonpath.JsonPath;
import com.np3tech.hoopmind.IntegrationTest;
import com.np3tech.hoopmind.TestAccounts;
import com.np3tech.hoopmind.TestAccounts.Session;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Compte de démonstration en lecture seule, et jetons d'accès refusés dès que le compte perd ses droits. */
@IntegrationTest
class DemoAndRevocationTests {

    @Autowired
    MockMvcTester mvc;

    @Test
    void aVisitorOpensADemoSessionThatCannotChangeTheSharedAccount() throws Exception {
        assertThat(mvc.get().uri("/api/v1/auth/demo")).hasStatusOk()
                .bodyJson().extractingPath("$.enabled").isEqualTo(true);

        MvcTestResult demo = mvc.post().uri("/api/v1/auth/demo").exchange();
        assertThat(demo).hasStatusOk();
        assertThat(demo.getResponse().getCookie(TestAccounts.REFRESH_COOKIE)).isNotNull();
        String body = demo.getResponse().getContentAsString();
        assertThat(JsonPath.<List<String>>read(body, "$.user.roles")).contains("ROLE_DEMO");
        String bearer = "Bearer " + JsonPath.read(body, "$.accessToken");

        assertThat(mvc.get().uri("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer)).hasStatusOk();
        assertThat(mvc.get().uri("/api/v1/predictions/mine").header(HttpHeaders.AUTHORIZATION, bearer)).hasStatusOk();
        assertThat(mvc.patch().uri("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"bio\":\"piraté\"}"))
                .hasStatus(403);
        assertThat(mvc.put().uri("/api/v1/users/me/password").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"x\",\"newPassword\":\"NewPassword456\"}"))
                .hasStatus(403);
        assertThat(mvc.delete().uri("/api/v1/predictions/mine/{id}", 1).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(403);
        assertThat(mvc.get().uri("/api/v1/admin/users").header(HttpHeaders.AUTHORIZATION, bearer)).hasStatus(403);
    }

    @Test
    void aDisabledAccountLosesItsAccessTokenImmediately() {
        Session user = TestAccounts.register(mvc);
        assertThat(mvc.get().uri("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, user.bearer())).hasStatusOk();

        assertThat(mvc.put().uri("/api/v1/admin/users/{id}/status", user.userId())
                .header(HttpHeaders.AUTHORIZATION, TestAccounts.admin(mvc).bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .hasStatusOk();

        assertThat(mvc.get().uri("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, user.bearer())).hasStatus(401);
    }

    @Test
    void anUnknownSortPropertyIsAClientError() {
        assertThat(mvc.get().uri("/api/v1/games").param("sort", "doesNotExist"))
                .hasStatus(400)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().extractingPath("$.detail").isEqualTo("Critère de tri invalide");
    }
}
