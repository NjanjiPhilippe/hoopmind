package com.np3tech.hoopmind.ml.explanation;

import com.np3tech.hoopmind.ml.GameOutcomePredictor;
import com.np3tech.hoopmind.nba.GameHistory;
import com.np3tech.hoopmind.nba.GameRecord;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class PredictionExplanationServiceTests {

    private static final long CELTICS = 1;
    private static final long KNICKS = 2;

    private final GameOutcomePredictor predictor = (season, home, away, date, playoff) -> Fixtures.analysis();
    private final AtomicInteger calls = new AtomicInteger();
    private final MeterRegistry registry = new SimpleMeterRegistry();

    @Test
    void withoutProviderExplainsWithoutLlm() {
        ExplainedPrediction result = service(null).explain(10, CELTICS, KNICKS, Fixtures.DAY, false);

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

        ExplainedPrediction first = service.explain(10, CELTICS, KNICKS, Fixtures.DAY, false);
        ExplainedPrediction second = service.explain(10, CELTICS, KNICKS, Fixtures.DAY, false);
        service.explain(10, CELTICS, KNICKS, Fixtures.DAY.plusDays(1), false);

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

        ExplainedPrediction result = service.explain(10, CELTICS, KNICKS, Fixtures.DAY, false);
        service.explain(10, CELTICS, KNICKS, Fixtures.DAY, false);

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

    private PredictionExplanationService service(PredictionExplainer explainer) {
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        if (explainer != null) {
            beans.addBean("explainer", explainer);
        }
        return new PredictionExplanationService(predictor, new Teams(), beans.getBeanProvider(PredictionExplainer.class),
                new ExplanationProperties("test", null, null, null, Duration.ofSeconds(5), 2048, 10, 4), registry);
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
