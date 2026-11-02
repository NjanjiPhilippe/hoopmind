package com.np3tech.hoopmind.ml.explanation;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LlmCallQuotaTests {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-02T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void limitsEachUserPerWindow() {
        LlmCallQuota quota = new LlmCallQuota(2, clock);

        assertThat(quota.tryAcquire("alice")).isTrue();
        assertThat(quota.tryAcquire("alice")).isTrue();
        assertThat(quota.tryAcquire("alice")).isFalse();
        assertThat(quota.tryAcquire("bob")).isTrue();
    }

    @Test
    void stopsTrackingNewUsersOnceTheTableIsFullOfOpenWindows() {
        LlmCallQuota quota = new LlmCallQuota(2, clock);
        for (int i = 0; i < 10_000; i++) {
            quota.tryAcquire("user-" + i);
        }

        assertThat(quota.tryAcquire("newcomer")).as("table pleine : explication sans LLM").isFalse();
        assertThat(quota.tryAcquire("user-0")).as("un utilisateur déjà suivi garde son quota").isTrue();
    }
}
