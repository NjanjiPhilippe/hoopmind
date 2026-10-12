package com.np3tech.hoopmind.ml.explanation;

/** Indisponibilité passagère (réseau, délai dépassé, quota 429, erreur 5xx) : l'appel est retenté. */
public final class LlmUnavailableException extends LlmException {

    public LlmUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
