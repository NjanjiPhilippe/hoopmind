package com.np3tech.hoopmind.ml.explanation;

/**
 * @param provider qui a rédigé le texte : {@code openai-compatible}, {@code anthropic} ou {@code template}
 * @param model    modèle de langage utilisé (absent pour {@code template})
 */
public record Explanation(String text, String provider, String model) {
}
