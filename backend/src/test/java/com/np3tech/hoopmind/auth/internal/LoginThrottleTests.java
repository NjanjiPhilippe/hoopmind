package com.np3tech.hoopmind.auth.internal;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LoginThrottleTests {

    private final MutableClock clock = new MutableClock();
    private final LoginThrottle throttle = new LoginThrottle(
            new LoginThrottleProperties(3, 10, Duration.ofSeconds(30), Duration.ofMinutes(15), Duration.ofMinutes(15)),
            clock);

    @Test
    void blocksAnAddressAfterTooManyFailuresOnTheSameLoginThenDoublesTheBlock() {
        fail("alice", "198.51.100.1", 2);
        assertThat(throttle.check("alice", "198.51.100.1")).isEmpty();

        fail("alice", "198.51.100.1", 1);
        assertThat(throttle.check("ALICE ", "198.51.100.1")).hasValueSatisfying(rejection -> {
            assertThat(rejection.retryAfter()).isEqualTo(Duration.ofSeconds(30));
            assertThat(rejection.firstRejection()).isTrue();
        });
        assertThat(throttle.check("alice", "198.51.100.1")).hasValueSatisfying(
                rejection -> assertThat(rejection.firstRejection()).as("un seul événement d'audit par blocage").isFalse());

        // Le vrai propriétaire, depuis une autre adresse, n'est pas bloqué.
        assertThat(throttle.check("alice", "203.0.113.9")).isEmpty();

        clock.advance(Duration.ofSeconds(31));
        assertThat(throttle.check("alice", "198.51.100.1")).isEmpty();
        fail("alice", "198.51.100.1", 1);
        assertThat(throttle.check("alice", "198.51.100.1"))
                .hasValueSatisfying(rejection -> assertThat(rejection.retryAfter()).isEqualTo(Duration.ofMinutes(1)));
    }

    @Test
    void blocksAnAddressThatTriesManyAccounts() {
        for (int i = 0; i < 10; i++) {
            fail("compte-" + i, "198.51.100.2", 1);
        }
        assertThat(throttle.check("encore-un-autre", "198.51.100.2")).isPresent();
    }

    @Test
    void aSuccessfulLoginForgetsTheFailuresOfThatLoginAndCountersExpire() {
        fail("bob", "198.51.100.3", 2);
        throttle.succeeded("bob", "198.51.100.3");
        fail("bob", "198.51.100.3", 2);
        assertThat(throttle.check("bob", "198.51.100.3")).isEmpty();

        clock.advance(Duration.ofMinutes(16));
        fail("bob", "198.51.100.3", 2);
        assertThat(throttle.check("bob", "198.51.100.3")).isEmpty();
    }

    @Test
    void theBlockNeverExceedsTheMaximum() {
        fail("carol", "198.51.100.4", 3);
        for (int i = 0; i < 30; i++) {
            // L'attaquant attend la fin de chaque blocage puis réessaie : le blocage continue de croître.
            clock.advance(throttle.check("carol", "198.51.100.4").orElseThrow().retryAfter().plusSeconds(1));
            fail("carol", "198.51.100.4", 1);
        }
        assertThat(throttle.check("carol", "198.51.100.4"))
                .hasValueSatisfying(rejection -> assertThat(rejection.retryAfter()).isEqualTo(Duration.ofMinutes(15)));
    }

    private void fail(String login, String address, int times) {
        for (int i = 0; i < times; i++) {
            throttle.failed(login, address);
        }
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-11T12:00:00Z");

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
