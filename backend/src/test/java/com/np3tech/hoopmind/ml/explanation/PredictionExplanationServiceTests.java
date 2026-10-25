package com.np3tech.hoopmind.ml.explanation;

import com.np3tech.hoopmind.ml.GameOutcomePredictor;
import com.np3tech.hoopmind.nba.GameHistory;
import com.np3tech.hoopmind.nba.GameRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class PredictionExplanationServiceTests {

    private static final long CELTICS = 1;
    private static final long KNICKS = 2;
    private static final String USER = "42";

    private final GameOutcomePredictor predictor = (season, home, away, date, playoff) -> Fixtures.analysis();
    private final AtomicInteger calls = new AtomicInteger();

    @Test
    void withoutProviderExplainsWithoutLlm() {
        ExplainedPrediction result = service(null).explain(USER, 10, CELTICS, KNICKS, Fixtures.DAY, false);

        assertThat(result.homeTeam()).isEqualTo("Boston Celtics");
        assertThat(result.awayTeam()).isEqualTo("New York Knicks");
        assertThat(result.prediction().homeWinProbability()).isEqualTo(0.71);
        assertThat(result.factors()).extracting(factor -> factor.feature())
                .containsExactly("net_rating_diff", "win_pct_diff", "rest_days_diff", "last10_win_pct_diff", "is_playoff");
        assertThat(result.explanation().provider()).isEqualTo("template");
        assertThat(result.fallback()).isFalse();
    }

    @Test
    void keepsGeneratedExplanationsToSaveTheQuota() {
        PredictionExplanationService service = service(request -> {
            calls.incrementAndGet();
            return new Explanation("Boston favori.", "openai-compatible", "gemini-2.5-flash");
        });

        ExplainedPrediction first = service.explain(USER, 10, CELTICS, KNICKS, Fixtures.DAY, false);
        ExplainedPrediction second = service.explain(USER, 10, CELTICS, KNICKS, Fixtures.DAY, false);
        service.explain(USER, 10, CELTICS, KNICKS, Fixtures.DAY.plusDays(1), false);

        assertThat(first.cached()).isFalse();
        assertThat(second.cached()).isTrue();
        assertThat(second.explanation().text()).isEqualTo("Boston favori.");
        assertThat(calls).as("une autre date est une autre question").hasValue(2);
    }

    @Test
    void fallsBackWhenTheLlmFailsAndAsksAgainNextTime() {
        PredictionExplanationService service = service(request -> {
            calls.incrementAndGet();
            throw new LlmUnavailableException("quota", null);
        });

        ExplainedPrediction result = service.explain(USER, 10, CELTICS, KNICKS, Fixtures.DAY, false);
        service.explain(USER, 10, CELTICS, KNICKS, Fixtures.DAY, false);

        assertThat(result.fallback()).isTrue();
        assertThat(result.explanation().provider()).isEqualTo("template");
        assertThat(result.explanation().text()).startsWith("Le modèle voit Boston Celtics gagner");
        assertThat(calls).as("un échec n'est pas gardé en mémoire").hasValue(2);
    }

    @Test
    void eachUserHasAnHourlyLimitOfLlmCallsThenGetsTheTemplate() {
        MutableClock clock = new MutableClock();
        PredictionExplanationService service = service(request -> {
            calls.incrementAndGet();
            return new Explanation("Boston favori.", "openai-compatible", "gemini-2.5-flash");
        }, 2, clock);

        service.explain(USER, 10, CELTICS, KNICKS, Fixtures.DAY, false);
        service.explain(USER, 10, CELTICS, KNICKS, Fixtures.DAY.plusDays(1), false);
        ExplainedPrediction limited = service.explain(USER, 10, CELTICS, KNICKS, Fixtures.DAY.plusDays(2), false);
        ExplainedPrediction cached = service.explain(USER, 10, CELTICS, KNICKS, Fixtures.DAY, false);
        ExplainedPrediction otherUser = service.explain("43", 10, CELTICS, KNICKS, Fixtures.DAY.plusDays(2), false);

        assertThat(limited.fallback()).isTrue();
        assertThat(limited.explanation().provider()).isEqualTo("template");
        assertThat(cached.cached()).as("une explication déjà générée ne coûte rien").isTrue();
        assertThat(otherUser.fallback()).as("la limite est propre à chaque utilisateur").isFalse();
        assertThat(calls).hasValue(3);

        clock.advance(LlmCallQuota.WINDOW);
        assertThat(service.explain(USER, 10, CELTICS, KNICKS, Fixtures.DAY.plusDays(3), false).fallback())
                .as("une heure plus tard").isFalse();
    }

    private PredictionExplanationService service(PredictionExplainer explainer) {
        return service(explainer, 20, Clock.systemUTC());
    }

    private PredictionExplanationService service(PredictionExplainer explainer, int maxCallsPerUserPerHour, Clock clock) {
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        if (explainer != null) {
            beans.addBean("explainer", explainer);
        }
        return new PredictionExplanationService(predictor, new Teams(), beans.getBeanProvider(PredictionExplainer.class),
                new ExplanationProperties("test", null, null, null, Duration.ofSeconds(5), 2048, 10, 4,
                        maxCallsPerUserPerHour), clock);
    }

    private static final class MutableClock extends Clock {

        private Instant now = Instant.parse("2026-10-22T20:00:00Z");

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

    private static final class Teams implements GameHistory {

        @Override
        public Map<Long, String> teamNames(Collection<Long> teamIds) {
            return Map.of(CELTICS, "Boston Celtics", KNICKS, "New York Knicks");
        }

        @Override
        public List<Integer> recentSeasonYears(int count) {
            return List.of();
        }

        @Override
        public List<GameRecord> games(Collection<Integer> seasonYears) {
            return List.of();
        }

        @Override
        public List<GameRecord> teamGames(int seasonYear, Collection<Long> teamIds) {
            return List.of();
        }

        @Override
        public Optional<Integer> seasonYear(long seasonId) {
            return Optional.of(2024);
        }
    }
}
