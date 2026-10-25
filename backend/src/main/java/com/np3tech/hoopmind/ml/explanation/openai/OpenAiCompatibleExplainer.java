package com.np3tech.hoopmind.ml.explanation.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.np3tech.hoopmind.ml.explanation.Explanation;
import com.np3tech.hoopmind.ml.explanation.ExplanationPrompt;
import com.np3tech.hoopmind.ml.explanation.ExplanationProperties;
import com.np3tech.hoopmind.ml.explanation.ExplanationRequest;
import com.np3tech.hoopmind.ml.explanation.LlmHttp;
import com.np3tech.hoopmind.ml.explanation.LlmRejectedException;
import com.np3tech.hoopmind.ml.explanation.LlmUnavailableException;
import com.np3tech.hoopmind.ml.explanation.PredictionExplainer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Adaptateur pour toute API compatible avec {@code POST /chat/completions} d'OpenAI. Un seul code couvre plusieurs
 * fournisseurs, en changeant l'adresse, la clé et le modèle :
 * <ul>
 *     <li>Google Gemini : {@code https://generativelanguage.googleapis.com/v1beta/openai} (offre gratuite) ;</li>
 *     <li>Groq : {@code https://api.groq.com/openai/v1} (offre gratuite) ;</li>
 *     <li>Ollama en local : {@code http://localhost:11434/v1} (sans clé) ;</li>
 *     <li>OpenAI : {@code https://api.openai.com/v1}.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(prefix = "hoopmind.ml.explanation", name = "provider", havingValue = OpenAiCompatibleExplainer.PROVIDER)
public class OpenAiCompatibleExplainer implements PredictionExplainer {

    public static final String PROVIDER = "openai-compatible";

    private final ExplanationProperties properties;
    private final RestClient client;

    public OpenAiCompatibleExplainer(ExplanationProperties properties) {
        if (properties.baseUrl() == null || properties.baseUrl().isBlank() || properties.model() == null
                || properties.model().isBlank()) {
            throw new IllegalStateException("Fournisseur " + PROVIDER + " : renseigner LLM_BASE_URL et LLM_MODEL.");
        }
        this.properties = properties;
        this.client = LlmHttp.client(properties.baseUrl(), properties, headers -> {
            if (properties.hasApiKey()) {
                headers.setBearerAuth(properties.apiKey());
            }
        });
    }

    @Override
    @ConcurrencyLimit(limitString = "${hoopmind.ml.explanation.max-concurrent-calls:4}")
    @Retryable(includes = LlmUnavailableException.class,
            maxRetriesString = "${hoopmind.ml.explanation.retry.max-retries:2}",
            delayString = "${hoopmind.ml.explanation.retry.delay-ms:1000}",
            multiplier = 2, jitter = 250, maxDelay = 8_000)
    public Explanation explain(ExplanationRequest request) {
        ChatRequest body = new ChatRequest(properties.model(), List.of(
                new Message("system", ExplanationPrompt.system()),
                new Message("user", ExplanationPrompt.user(request))),
                0.3, properties.maxTokens());
        ChatResponse response;
        try {
            response = client.post().uri("/chat/completions").body(body).retrieve().body(ChatResponse.class);
        } catch (RestClientException e) {
            throw LlmHttp.translate(PROVIDER, e);
        }
        String text = text(response);
        Usage usage = response.usage();
        return new Explanation(text, PROVIDER, properties.model(),
                usage == null ? null : usage.promptTokens(), usage == null ? null : usage.completionTokens());
    }

    private static String text(ChatResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()
                || response.choices().getFirst().message() == null) {
            throw new LlmRejectedException(PROVIDER + " : réponse sans texte");
        }
        String content = response.choices().getFirst().message().content();
        if (content == null || content.isBlank()) {
            throw new LlmRejectedException(PROVIDER + " : réponse vide (" + response.choices().getFirst().finishReason() + ")");
        }
        return content.strip();
    }

    record ChatRequest(String model, List<Message> messages, double temperature,
                       @JsonProperty("max_tokens") int maxTokens) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatResponse(List<Choice> choices, Usage usage) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Usage(@JsonProperty("prompt_tokens") Integer promptTokens,
                 @JsonProperty("completion_tokens") Integer completionTokens) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(Message message, @JsonProperty("finish_reason") String finishReason) {
    }
}
