package com.np3tech.hoopmind.ml.explanation.anthropic;

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
import java.util.stream.Collectors;

/** Adaptateur pour l'API Messages d'Anthropic ({@code POST /v1/messages}), donc les modèles Claude. */
@Component
@ConditionalOnProperty(prefix = "hoopmind.ml.explanation", name = "provider", havingValue = AnthropicExplainer.PROVIDER)
public class AnthropicExplainer implements PredictionExplainer {

    public static final String PROVIDER = "anthropic";

    static final String DEFAULT_BASE_URL = "https://api.anthropic.com/v1";
    static final String DEFAULT_MODEL = "claude-haiku-5-5";
    private static final String API_VERSION = "2023-06-01";

    private final RestClient client;
    private final String model;
    private final int maxTokens;

    public AnthropicExplainer(ExplanationProperties properties) {
        if (!properties.hasApiKey()) {
            throw new IllegalStateException("Fournisseur " + PROVIDER + " : renseigner LLM_API_KEY.");
        }
        String baseUrl = properties.baseUrl() == null || properties.baseUrl().isBlank() ? DEFAULT_BASE_URL : properties.baseUrl();
        this.model = properties.model() == null || properties.model().isBlank() ? DEFAULT_MODEL : properties.model();
        this.maxTokens = properties.maxTokens();
        this.client = LlmHttp.client(baseUrl, properties, headers -> {
            headers.set("x-api-key", properties.apiKey());
            headers.set("anthropic-version", API_VERSION);
        });
    }

    @Override
    @ConcurrencyLimit(limitString = "${hoopmind.ml.explanation.max-concurrent-calls:4}")
    @Retryable(includes = LlmUnavailableException.class,
            maxRetriesString = "${hoopmind.ml.explanation.retry.max-retries:2}",
            delayString = "${hoopmind.ml.explanation.retry.delay-ms:1000}",
            multiplier = 2, jitter = 250, maxDelay = 8_000)
    public Explanation explain(ExplanationRequest request) {
        MessagesRequest body = new MessagesRequest(model, maxTokens, ExplanationPrompt.system(),
                List.of(new Message("user", ExplanationPrompt.user(request))));
        MessagesResponse response;
        try {
            response = client.post().uri("/messages").body(body).retrieve().body(MessagesResponse.class);
        } catch (RestClientException e) {
            throw LlmHttp.translate(PROVIDER, e);
        }
        String text = text(response);
        Usage usage = response.usage();
        return new Explanation(text, PROVIDER, model,
                usage == null ? null : usage.inputTokens(), usage == null ? null : usage.outputTokens());
    }

    /** Seuls les blocs de texte comptent (le raisonnement éventuel du modèle arrive dans d'autres blocs). */
    private static String text(MessagesResponse response) {
        if (response == null || response.content() == null) {
            throw new LlmRejectedException(PROVIDER + " : réponse sans contenu");
        }
        if ("refusal".equals(response.stopReason())) {
            throw new LlmRejectedException(PROVIDER + " : le modèle a refusé de répondre");
        }
        String text = response.content().stream()
                .filter(block -> "text".equals(block.type()) && block.text() != null)
                .map(Block::text)
                .collect(Collectors.joining()).strip();
        if (text.isEmpty()) {
            throw new LlmRejectedException(PROVIDER + " : réponse vide (" + response.stopReason() + ")");
        }
        return text;
    }

    record MessagesRequest(String model, @JsonProperty("max_tokens") int maxTokens, String system,
                           List<Message> messages) {
    }

    record Message(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MessagesResponse(List<Block> content, @JsonProperty("stop_reason") String stopReason, Usage usage) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Usage(@JsonProperty("input_tokens") Integer inputTokens, @JsonProperty("output_tokens") Integer outputTokens) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Block(String type, String text) {
    }
}
