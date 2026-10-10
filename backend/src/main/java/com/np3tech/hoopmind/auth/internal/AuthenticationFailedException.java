package com.np3tech.hoopmind.auth.internal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import java.net.URI;

/**
 * Identifiants ou jeton de rafraîchissement refusés. Le message reste volontairement générique.
 */
public class AuthenticationFailedException extends ErrorResponseException {

    AuthenticationFailedException(String type, String detail) {
        super(HttpStatus.UNAUTHORIZED, problem(type, detail), null);
    }

    private static ProblemDetail problem(String type, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail);
        problem.setType(URI.create("https://hoopmind.dev/problems/" + type));
        problem.setTitle("Authentification refusée");
        return problem;
    }

    static AuthenticationFailedException badCredentials() {
        return new AuthenticationFailedException("bad-credentials", "Identifiant ou mot de passe incorrect");
    }

    static AuthenticationFailedException invalidRefreshToken() {
        return new AuthenticationFailedException("invalid-refresh-token", "Session expirée, reconnectez-vous");
    }
}
