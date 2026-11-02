package com.np3tech.hoopmind.ml.explanation;

/**
 * Quota du fournisseur atteint (HTTP 429). Pas retenté : sur une offre gratuite, le quota se compte à la minute ou
 * à la journée, et chaque nouvel essai le consomme encore. L'explication rédigée sans LLM répond tout de suite.
 */
public final class LlmQuotaExceededException extends LlmException {

    public LlmQuotaExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
