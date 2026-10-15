package com.np3tech.hoopmind.ml.explanation;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;

/**
 * Métriques des explications, exposées sur {@code /actuator/prometheus} :
 * <ul>
 *     <li>{@code hoopmind.explanations} : explications servies, par origine ({@code llm}, {@code cache},
 *     {@code template} sans LLM configuré, {@code fallback} quand le LLM a échoué) ;</li>
 *     <li>{@code hoopmind.llm.calls} : durée des appels au LLM (nouvelles tentatives comprises), par fournisseur
 *     et résultat ({@code success}, {@code unavailable}, {@code rejected}) ;</li>
 *     <li>{@code hoopmind.llm.tokens} : tokens consommés, par fournisseur, modèle et sens ({@code input},
 *     {@code output}), quand le fournisseur les indique.</li>
 * </ul>
 */
class ExplanationMetrics {

    enum Source { LLM, CACHE, TEMPLATE, FALLBACK }

    private final MeterRegistry registry;
    private final String provider;

    ExplanationMetrics(MeterRegistry registry, String provider) {
        this.registry = registry;
        this.provider = provider == null || provider.isBlank() ? "none" : provider;
    }

    void served(Source source) {
        Counter.builder("hoopmind.explanations")
                .description("Explications de prédiction servies, par origine")
                .tag("source", source.name().toLowerCase())
                .register(registry).increment();
    }

    void call(Duration duration, LlmException failure) {
        String outcome = failure == null ? "success" : failure instanceof LlmRejectedException ? "rejected" : "unavailable";
        Timer.builder("hoopmind.llm.calls")
                .description("Appels au LLM qui rédige les explications")
                .tag("provider", provider)
                .tag("outcome", outcome)
                .publishPercentileHistogram()
                .register(registry).record(duration);
    }

    void tokens(Explanation explanation) {
        tokens(explanation, "input", explanation.inputTokens());
        tokens(explanation, "output", explanation.outputTokens());
    }

    private void tokens(Explanation explanation, String direction, Integer count) {
        if (count == null || count <= 0) {
            return;
        }
        Counter.builder("hoopmind.llm.tokens")
                .description("Tokens consommés par le LLM")
                .tag("provider", explanation.provider())
                .tag("model", explanation.model() == null ? "unknown" : explanation.model())
                .tag("direction", direction)
                .register(registry).increment(count);
    }
}
