package com.np3tech.hoopmind;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

@IntegrationTest
class ApplicationStartupTests {

    @Autowired
    MockMvcTester mvc;

    @Test
    void healthIsPublic() {
        assertThat(mvc.get().uri("/actuator/health")).hasStatusOk();
    }

    @Test
    void prometheusMetricsNeedTheScrapeCredentials() {
        assertThat(mvc.get().uri("/actuator/prometheus")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().uri("/actuator/prometheus").with(httpBasic("prometheus", "wrong")))
                .hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().uri("/actuator/prometheus").with(httpBasic("prometheus", "test-metrics")))
                .hasStatusOk().bodyText().contains("http_server_requests_seconds", "application=\"hoopmind\"");
    }

    @Test
    void nbaReadEndpointsArePublic() {
        assertThat(mvc.get().uri("/api/v1/teams")).hasStatusOk()
                .bodyJson().extractingPath("$.length()").asNumber().isEqualTo(30);
    }
}
