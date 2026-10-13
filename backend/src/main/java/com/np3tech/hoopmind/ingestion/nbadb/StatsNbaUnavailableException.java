package com.np3tech.hoopmind.ingestion.nbadb;

/**
 * stats.nba.com n'a pas répondu correctement pour une raison qui peut être passagère (délai dépassé, coupure réseau,
 * HTTP 429 ou 5xx) : la requête est retentée. Une réponse illisible ne l'est pas.
 */
class StatsNbaUnavailableException extends NbaImportException {

    StatsNbaUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
