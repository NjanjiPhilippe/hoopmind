package com.np3tech.hoopmind.ml.explanation;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @param provider     qui a rédigé le texte : {@code openai-compatible}, {@code anthropic} ou {@code template}
 * @param model        modèle de langage utilisé (absent pour {@code template})
 * @param inputTokens  tokens lus par le LLM, si le fournisseur les indique (pour le monitoring, pas renvoyé au client)
 * @param outputTokens tokens écrits par le LLM, si le fournisseur les indique
 */
public record Explanation(String text, String provider, String model,
                          @JsonIgnore Integer inputTokens, @JsonIgnore Integer outputTokens) {

    public Explanation(String text, String provider, String model) {
        this(text, provider, model, null, null);
    }
}
