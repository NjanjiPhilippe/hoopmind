package com.np3tech.hoopmind.ml.explanation;

import com.np3tech.hoopmind.ml.GameOutcomeAnalysis;
import com.np3tech.hoopmind.ml.GameOutcomePredictor;
import com.np3tech.hoopmind.ml.explanation.ExplanationMetrics.Source;
import com.np3tech.hoopmind.nba.GameHistory;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Prédit un match puis l'explique. Le LLM n'est qu'un rédacteur : la probabilité et le poids de chaque facteur
 * viennent du modèle entraîné, et l'explication reste disponible sans lui.
 * <ul>
 *     <li>Une explication générée est gardée en mémoire (match, date, version du modèle) : les offres gratuites
 *     limitent le nombre d'appels, et la même question ne doit pas les consommer deux fois.</li>
 *     <li>Si le LLM ne répond pas (quota, panne, clé refusée), une explication rédigée sans LLM la remplace.</li>
 *     <li>Chaque explication est comptée par origine, et chaque appel au LLM mesuré ({@link ExplanationMetrics}).</li>
 * </ul>
 */
@Service
@EnableConfigurationProperties(ExplanationProperties.class)
@Slf4j
public class PredictionExplanationService {

    private final GameOutcomePredictor predictor;
    private final GameHistory history;
    private final PredictionExplainer explainer;
    private final Map<Key, Explanation> cache;
    private final ExplanationMetrics metrics;

    public PredictionExplanationService(GameOutcomePredictor predictor, GameHistory history,
                                        ObjectProvider<PredictionExplainer> explainer, ExplanationProperties properties,
                                        MeterRegistry registry) {
        this.predictor = predictor;
        this.history = history;
        this.explainer = explainer.getIfAvailable();
        int capacity = properties.cacheSize();
        this.cache = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Key, Explanation> eldest) {
                return size() > capacity;
            }
        });
        this.metrics = new ExplanationMetrics(registry, properties.provider());
        log.info("Explication des prédictions : {}", this.explainer == null
                ? "sans LLM (hoopmind.ml.explanation.provider=none)" : properties.provider());
    }

    public ExplainedPrediction explain(long seasonId, long homeTeamId, long awayTeamId, LocalDate date, boolean playoff) {
        GameOutcomeAnalysis analysis = predictor.analyze(seasonId, homeTeamId, awayTeamId, date, playoff);
        Map<Long, String> names = history.teamNames(List.of(homeTeamId, awayTeamId));
        ExplanationRequest request = new ExplanationRequest(
                names.getOrDefault(homeTeamId, "Équipe " + homeTeamId),
                names.getOrDefault(awayTeamId, "Équipe " + awayTeamId),
                date, playoff, analysis);

        if (explainer == null) {
            metrics.served(Source.TEMPLATE);
            return result(request, TemplateExplanation.of(request), false, false);
        }
        Key key = new Key(analysis.modelVersion(), seasonId, homeTeamId, awayTeamId, date, playoff);
        Explanation cached = cache.get(key);
        if (cached != null) {
            metrics.served(Source.CACHE);
            return result(request, cached, true, false);
        }
        long start = System.nanoTime();
        try {
            Explanation generated = explainer.explain(request);
            metrics.call(Duration.ofNanos(System.nanoTime() - start), null);
            metrics.tokens(generated);
            metrics.served(Source.LLM);
            cache.put(key, generated);
            return result(request, generated, false, false);
        } catch (LlmException e) {
            metrics.call(Duration.ofNanos(System.nanoTime() - start), e);
            metrics.served(Source.FALLBACK);
            log.warn("Explication par LLM impossible, explication sans LLM à la place : {}", e.getMessage());
            return result(request, TemplateExplanation.of(request), false, true);
        }
    }

    private static ExplainedPrediction result(ExplanationRequest request, Explanation explanation, boolean cached,
                                              boolean fallback) {
        return new ExplainedPrediction(request.homeTeam(), request.awayTeam(), request.analysis().prediction(),
                ExplanationFacts.byInfluence(request.analysis().factors()), explanation, cached, fallback);
    }

    private record Key(String modelVersion, long seasonId, long homeTeamId, long awayTeamId, LocalDate date,
                       boolean playoff) {
    }
}
