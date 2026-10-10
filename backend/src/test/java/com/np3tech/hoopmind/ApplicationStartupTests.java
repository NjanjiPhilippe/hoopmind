package com.np3tech.hoopmind;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class ApplicationStartupTests {

    @Autowired
    MockMvcTester mvc;

    @Test
    void healthIsPublic() {
        assertThat(mvc.get().uri("/actuator/health")).hasStatusOk();
    }

    @Test
    void nbaReadEndpointsArePublic() {
        assertThat(mvc.get().uri("/api/v1/teams")).hasStatusOk()
                .bodyJson().extractingPath("$.length()").asNumber().isEqualTo(30);
    }
}
