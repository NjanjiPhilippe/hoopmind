package com.np3tech.hoopmind.ml.explanation;

import com.np3tech.hoopmind.ml.explanation.anthropic.AnthropicExplainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnthropicExplainerTests {

    private final FakeLlmServer llm = new FakeLlmServer();

    @AfterEach
    void stop() {
        llm.close();
    }

    @Test
    void callsTheMessagesApiAndKeepsOnlyTextBlocks() {
        llm.reply(200, """
                {"id": "msg_1", "type": "message", "role": "assistant", "stop_reason": "end_turn",
                 "content": [{"type": "thinking", "thinking": "", "signature": "abc"},
                             {"type": "text", "text": "Boston part favori."},
                             {"type": "text", "text": " Le repos joue pour New York."}],
                 "usage": {"input_tokens": 400, "output_tokens": 60}}""");

        Explanation explanation = explainer(null).explain(Fixtures.request());

        assertThat(explanation).isEqualTo(new Explanation("Boston part favori. Le repos joue pour New York.",
                "anthropic", "claude-haiku-5-5"));
        FakeLlmServer.Received request = llm.received.getFirst();
        assertThat(request.path()).isEqualTo("/v1/messages");
        assertThat(request.headers().getFirst("x-api-key")).isEqualTo("secret");
        assertThat(request.headers().getFirst("anthropic-version")).isEqualTo("2023-06-01");
        assertThat(request.headers().containsKey("Authorization")).isFalse();
        assertThat(request.body()).contains("\"model\":\"claude-haiku-5-5\"", "\"max_tokens\":2048", "\"system\":",
                "<donnees>");
    }

    @Test
    void refusalIsARejection() {
        llm.reply(200, "{\"content\": [], \"stop_reason\": \"refusal\"}");

        assertThatThrownBy(() -> explainer(null).explain(Fixtures.request()))
                .isInstanceOf(LlmRejectedException.class).hasMessageContaining("refusé");
    }

    @Test
    void overloadIsTemporary() {
        llm.reply(529, "{\"type\": \"error\", \"error\": {\"type\": \"overloaded_error\"}}");

        assertThatThrownBy(() -> explainer(null).explain(Fixtures.request())).isInstanceOf(LlmUnavailableException.class);
    }

    @Test
    void requiresAKey() {
        assertThatThrownBy(() -> new AnthropicExplainer(new ExplanationProperties("anthropic", null, " ", null,
                Duration.ofSeconds(5), 2048, 10, 4))).isInstanceOf(IllegalStateException.class);
    }

    private AnthropicExplainer explainer(String model) {
        return new AnthropicExplainer(new ExplanationProperties("anthropic", llm.url("/v1"), "secret", model,
                Duration.ofSeconds(5), 2048, 10, 4));
    }
}
