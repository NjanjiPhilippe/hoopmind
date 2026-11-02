package com.np3tech.hoopmind.auth.internal;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AddressRateLimitTests {

    private final MutableClock clock = new MutableClock();

    @Test
    void limitsEachAddressPerWindowAndSaysHowLongToWait() {
        AddressRateLimit limit = new AddressRateLimit(2, Duration.ofHours(1), clock);

        assertThat(limit.tryAcquire("203.0.113.7")).isEmpty();
        assertThat(limit.tryAcquire("203.0.113.7")).isEmpty();
        clock.advance(Duration.ofMinutes(20));
        assertThat(limit.tryAcquire("203.0.113.7")).contains(Duration.ofMinutes(40));
        assertThat(limit.tryAcquire("198.51.100.1")).as("une autre adresse").isEmpty();

        clock.advance(Duration.ofMinutes(40));
        assertThat(limit.tryAcquire("203.0.113.7")).as("une heure après la première").isEmpty();
    }

    @Test
    void registrationsAndDemoSessionsHaveTheirOwnLimit() {
        SessionCreationLimits limits = new SessionCreationLimits(1, 1, clock);

        limits.registration("203.0.113.7");
        limits.demoSession("203.0.113.7");

        assertThatThrownBy(() -> limits.registration("203.0.113.7"))
                .isInstanceOfSatisfying(LoginThrottledException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(429));
    }

    private static final class MutableClock extends Clock {

        private Instant now = Instant.parse("2026-11-02T10:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
