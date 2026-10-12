package com.np3tech.hoopmind.ml.explanation;

/** Échec d'un appel au LLM. Le service se rabat alors sur une explication rédigée sans LLM. */
public abstract sealed class LlmException extends RuntimeException
        permits LlmUnavailableException, LlmRejectedException {

    protected LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
