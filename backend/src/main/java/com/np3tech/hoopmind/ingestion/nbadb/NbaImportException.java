package com.np3tech.hoopmind.ingestion.nbadb;

import com.np3tech.hoopmind.shared.exception.AppException;
import org.springframework.http.HttpStatus;

/** L'import n'a pas pu lire la base NBA (fichier absent, illisible ou schéma inattendu). */
public class NbaImportException extends AppException {

    private static final String CODE = "NBA_IMPORT_FAILED";

    public NbaImportException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_CONTENT, CODE);
    }

    public NbaImportException(String message, Throwable cause) {
        super(message, cause, HttpStatus.UNPROCESSABLE_CONTENT, CODE);
    }
}
