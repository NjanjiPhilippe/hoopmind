package com.np3tech.hoopmind.ml.explanation;

import com.np3tech.hoopmind.ml.GameOutcomeAnalysis;
import com.np3tech.hoopmind.ml.GameOutcomePredictor;
import com.np3tech.hoopmind.ml.PredictionFactor;
import com.np3tech.hoopmind.nba.GameHistory;
import com.np3tech.hoopmind.nba.GameRecord;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class PredictionExplanationServiceTests {

    private static final long CELTICS = 1;
    private static final long KNICKS = 2;
    private static final String USER = "42";

    /** Ce que le modèle répond ; un test peut le changer (réentraînement, nouveau résultat). */
    private final AtomicReference<GameOutcomeAnalysis> analysis = new AtomicReference<>(Fixtures.analysis());
    private final GameOutcomePredictor predictor = (season, home, away, date, playoff) -> analysis.get();
    private final AtomicInteger calls = new AtomicInteger();
    private final MeterRegistry registry = new SimpleMeterRegistry();

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
        assertThat(served("template")).isEqualTo(1);
    }

    @Test
    void keepsGeneratedExplanationsToSaveTheQuota() {
        PredictionExplanationService service = service(request -> {
            calls.incrementAndGet();
            return new Explanation("Boston favori.", "openai-compatible", "gemini-2.5-flash", 300, 50);
        });

        ExplainedPrediction first = service.explain(USER, 10, CELTICS, KNICKS, Fixtures.DAY, false);
        ExplainedPrediction second = service.explain(USER, 10, CELTICS, KNICKS, Fixtures.DAY, false);
        service.explain(USER, 10, CELTICS, KNICKS, Fixtures.DAY.plusDays(1), false);

        assertThat(first.cached()).isFalse();
        assertThat(second.cached()).isTrue();
        assertThat(second.explanation().text()).isEqualTo("Boston favori.");
        assertThat(calls).as("une autre date est une autre question").hasValue(2);
        assertThat(served("llm")).isEqualTo(2);
        assertThat(served("cache")).isEqualTo(1);
        assertThat(registry.get("hoopmind.llm.calls").tag("outcome", "success").timer().count()).isEqualTo(2);
        assertThat(registry.get("hoopmind.llm.tokens").tag("model", "gemini-2.5-flash").tag("direction", "input")
                .counter().count()).isEqualTo(600);
        assertThat(registry.get("hoopmind.llm.tokens").tag("direction", "output").counter().count()).isEqualTo(100);
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
        assertThat(served("fallback")).isEqualTo(2);
        assertThat(registry.get("hoopmind.llm.calls").tag("outcome", "unavailable").timer().count()).isEqualTo(2);
    }

    private double served(String source) {
        return registry.get("hoopmind.explanations").tag("source", source).counter().count();
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

    @Test
    void allUsersShareADailyLimitOfLlmCalls() {
        MutableClock clock = new MutableClock();
        PredictionExplanationService service = service(request -> {
            calls.incrementAndGet();
            return new Explanation("Boston favori.", "openai-compatible", "gemini-2.5-flash");
        }, 20, 2, clock);

        service.explain("1", 10, CELTICS, KNICKS, Fixtures.DAY, false);
        service.explain("2", 10, CELTICS, KNICKS, Fixtures.DAY.plusDays(1), false);
        ExplainedPrediction limited = service.explain("3", 10, CELTICS, KNICKS, Fixtures.DAY.plusDays(2), false);

        assertThat(limited.fallback()).as("plafond commun atteint, même pour un nouvel utilisateur").isTrue();
        assertThat(calls).hasValue(2);

        clock.advance(Duration.ofDays(1));
        assertThat(service.explain("3", 10, CELTICS, KNICKS, Fixtures.DAY.plusDays(2), false).fallback())
                .as("le lendemain").isFalse();
    }

    @Test
    void twoSimultaneousRequestsForTheSameExplanationMakeOneLlmCall() throws Exception {
        CountDownLatch called = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        PredictionExplanationService service = service(request -> {
            calls.incrementAndGet();
            called.countDown();
            await(release);
            return new Explanation("Boston favori.", "openai-compatible", "gemini-2.5-flash");
        });

        try (ExecutorService threads = Executors.newFixedThreadPool(2)) {
            Future<ExplainedPrediction> first = threads.submit(() -> service.explain("1", 10, CELTICS, KNICKS, Fixtures.DAY, false));
            assertThat(called.await(5, TimeUnit.SECONDS)).isTrue();
            Future<ExplainedPrediction> second = threads.submit(() -> service.explain("2", 10, CELTICS, KNICKS, Fixtures.DAY, false));
            Thread.sleep(100);
            release.countDown();

            assertThat(first.get(5, TimeUnit.SECONDS).cached()).isFalse();
            assertThat(second.get(5, TimeUnit.SECONDS).explanation().text()).isEqualTo("Boston favori.");
        }
        assertThat(calls).hasValue(1);
    }

    @Test
    void aRetrainedModelOrANewResultGetsANewExplanation() {
        PredictionExplanationService service = service(request -> {
            calls.incrementAndGet();
            return new Explanation("Boston favori.", "openai-compatible", "gemini-2.5-flash");
        });
        service.explain(USER, 10, CELTICS, KNICKS, Fixtures.DAY, false);

        GameOutcomeAnalysis before = Fixtures.analysis();
        analysis.set(new GameOutcomeAnalysis(before.prediction(), "v2", before.intercept(), before.factors(),
                before.homeGamesPlayed(), before.awayGamesPlayed()));
        assertThat(service.explain(USER, 10, CELTICS, KNICKS, Fixtures.DAY, false).cached())
                .as("modèle réentraîné").isFalse();

        List<PredictionFactor> newForm = new ArrayList<>(before.factors());
        PredictionFactor rest = newForm.get(3);
        newForm.set(3, new PredictionFactor(rest.feature(), rest.homeValue() + 1, rest.awayValue(), rest.contribution()));
        analysis.set(new GameOutcomeAnalysis(before.prediction(), "v2", before.intercept(), newForm,
                before.homeGamesPlayed() + 1, before.awayGamesPlayed()));
        assertThat(service.explain(USER, 10, CELTICS, KNICKS, Fixtures.DAY, false).cached())
                .as("un résultat saisi change la forme").isFalse();
        assertThat(calls).hasValue(3);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private PredictionExplanationService service(PredictionExplainer explainer) {
        return service(explainer, 20, Clock.systemUTC());
    }

    private PredictionExplanationService service(PredictionExplainer explainer, int maxCallsPerUserPerHour, Clock clock) {
        return service(explainer, maxCallsPerUserPerHour, 200, clock);
    }

    private PredictionExplanationService service(PredictionExplainer explainer, int maxCallsPerUserPerHour,
                                                 int maxCallsPerDay, Clock clock) {
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        if (explainer != null) {
            beans.addBean("explainer", explainer);
        }
        return new PredictionExplanationService(predictor, new Teams(), beans.getBeanProvider(PredictionExplainer.class),
                new ExplanationProperties("test", null, null, null, Duration.ofSeconds(5), 2048, 10, 4,
                        maxCallsPerUserPerHour, maxCallsPerDay), clock, registry);
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
