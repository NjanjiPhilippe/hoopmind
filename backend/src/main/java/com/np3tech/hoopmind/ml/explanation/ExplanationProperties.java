package com.np3tech.hoopmind.ml.explanation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Réglages de l'explication des prédictions ({@code hoopmind.ml.explanation.*}).
 *
 * @param provider           {@code none} (explication sans LLM), {@code openai-compatible} (Gemini, Groq, Ollama,
 *                           OpenAI…) ou {@code anthropic} (Claude)
 * @param baseUrl            adresse de l'API (celle d'Anthropic par défaut pour {@code anthropic})
 * @param apiKey             clé d'API (vide pour Ollama en local)
 * @param model              modèle de langage, par exemple {@code gemini-2.5-flash} ou {@code claude-haiku-5-5}
 * @param timeout            délai maximal d'un appel
 * @param maxTokens          longueur maximale de la réponse, raisonnement du modèle compris
 * @param cacheSize          nombre d'explications gardées en mémoire (une par match, date et version du modèle)
 * @param maxConcurrentCalls appels simultanés au LLM au plus (les suivants attendent leur tour)
 */
@ConfigurationProperties("hoopmind.ml.explanation")
public record ExplanationProperties(
        @DefaultValue("none") String provider,
        String baseUrl,
        String apiKey,
        String model,
        @DefaultValue("20s") Duration timeout,
        @DefaultValue("2048") int maxTokens,
        @DefaultValue("500") int cacheSize,
        @DefaultValue("4") int maxConcurrentCalls
) {

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
