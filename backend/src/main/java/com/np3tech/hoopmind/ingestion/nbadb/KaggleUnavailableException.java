package com.np3tech.hoopmind.ingestion.nbadb;

/**
 * Kaggle n'a pas répondu correctement pour une raison qui peut être passagère (coupure réseau, HTTP 429 ou 5xx) :
 * le téléchargement est retenté. Une clé refusée ou un jeu de données introuvable ne l'est pas.
 */
class KaggleUnavailableException extends NbaImportException {

    KaggleUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
