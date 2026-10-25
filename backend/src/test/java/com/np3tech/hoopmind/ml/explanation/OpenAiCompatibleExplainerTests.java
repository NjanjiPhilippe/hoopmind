package com.np3tech.hoopmind.ml.explanation;

import com.np3tech.hoopmind.ml.explanation.openai.OpenAiCompatibleExplainer;
import com.np3tech.hoopmind.shared.config.ResilienceConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiCompatibleExplainerTests {

    private static final String ANSWER = """
            {"id": "x", "choices": [{"index": 0, "finish_reason": "stop",
              "message": {"role": "assistant", "content": "  Boston part favori grâce à son net rating.  "}}],
             "usage": {"prompt_tokens": 280, "completion_tokens": 41, "total_tokens": 321}}""";

    private final FakeLlmServer llm = new FakeLlmServer();

    @AfterEach
    void stop() {
        llm.close();
    }

    @Test
    void sendsThePromptToChatCompletionsAndReadsTheAnswer() {
        llm.reply(200, ANSWER);

        Explanation explanation = explainer("secret").explain(Fixtures.request());

        assertThat(explanation).isEqualTo(new Explanation("Boston part favori grâce à son net rating.",
                "openai-compatible", "gemini-2.5-flash", 280, 41));
        FakeLlmServer.Received request = llm.received.getFirst();
        assertThat(request.path()).isEqualTo("/v1beta/openai/chat/completions");
        assertThat(request.headers().getFirst("Authorization")).isEqualTo("Bearer secret");
        assertThat(request.body()).contains("\"model\":\"gemini-2.5-flash\"", "\"max_tokens\":2048",
                "\"role\":\"system\"", "\"role\":\"user\"", "<donnees>");
    }

    @Test
    void sendsNoAuthorizationWithoutKey() {
        llm.reply(200, ANSWER);

        explainer(null).explain(Fixtures.request());

        assertThat(llm.received.getFirst().headers().containsKey("Authorization")).as("Ollama en local").isFalse();
    }

    @Test
    void refusedKeyAndEmptyAnswerAreRejections() {
        llm.reply(401, "{\"error\": {\"message\": \"invalid key\"}}")
                .reply(200, "{\"choices\": [{\"finish_reason\": \"length\", \"message\": {\"content\": \"\"}}]}");

        assertThatThrownBy(() -> explainer("bad").explain(Fixtures.request()))
                .isInstanceOf(LlmRejectedException.class).hasMessageContaining("401");
        assertThatThrownBy(() -> explainer("secret").explain(Fixtures.request()))
                .isInstanceOf(LlmRejectedException.class).hasMessageContaining("length");
    }

    @Test
    void quotaAndServerErrorsAreTemporary() {
        llm.reply(429, "{}").reply(503, "{}");

        assertThatThrownBy(() -> explainer("secret").explain(Fixtures.request())).isInstanceOf(LlmUnavailableException.class);
        assertThatThrownBy(() -> explainer("secret").explain(Fixtures.request())).isInstanceOf(LlmUnavailableException.class);
    }

    /** {@code @Retryable} vérifié à travers le vrai proxy Spring : deux 429 puis une réponse donnent 3 appels. */
    @Test
    void retriesTemporaryErrorsButNotRejections() {
        llm.reply(429, "{}").reply(429, "{}").reply(200, ANSWER).reply(401, "{}");
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test",
                    Map.of("hoopmind.ml.explanation.provider", "openai-compatible",
                            "hoopmind.ml.explanation.retry.delay-ms", "10")));
            context.register(ResilienceConfig.class, OpenAiCompatibleExplainer.class);
            context.registerBean(ExplanationProperties.class, () -> properties("secret"));
            context.refresh();
            PredictionExplainer explainer = context.getBean(PredictionExplainer.class);

            assertThat(explainer.explain(Fixtures.request()).text()).startsWith("Boston");
            assertThat(llm.received).hasSize(3);

            assertThatThrownBy(() -> explainer.explain(Fixtures.request())).isInstanceOf(LlmRejectedException.class);
            assertThat(llm.received).hasSize(4);
        }
    }

    private OpenAiCompatibleExplainer explainer(String key) {
        return new OpenAiCompatibleExplainer(properties(key));
    }

    private ExplanationProperties properties(String key) {
        return new ExplanationProperties("openai-compatible", llm.url("/v1beta/openai"), key, "gemini-2.5-flash",
                Duration.ofSeconds(5), 2048, 10, 4, 20);
    }
}
