package com.np3tech.hoopmind.ml.explanation;

/** Demande refusée (clé invalide, modèle inconnu, réponse vide ou refusée) : la retenter ne changerait rien. */
public final class LlmRejectedException extends LlmException {

    public LlmRejectedException(String message) {
        super(message, null);
    }

    public LlmRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
