package com.np3tech.hoopmind.auth.internal;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import java.net.URI;
import java.time.Duration;

/**
 * Trop d'échecs de connexion récents, ou trop de comptes et de sessions de démonstration créés depuis une même
 * adresse : 429 avec l'en-tête {@code Retry-After} (en secondes) et le code {@code TOO_MANY_ATTEMPTS}. Le message
 * ne dit pas si le compte existe.
 */
public class LoginThrottledException extends ErrorResponseException {

    private final HttpHeaders headers = new HttpHeaders();

    LoginThrottledException(Duration retryAfter) {
        this(retryAfter, "Trop de tentatives de connexion.");
    }

    /** @param reason première phrase du message, suivie du délai d'attente */
    LoginThrottledException(Duration retryAfter, String reason) {
        super(HttpStatus.TOO_MANY_REQUESTS, problem(seconds(retryAfter), reason), null);
        headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(seconds(retryAfter)));
    }

    @Override
    public HttpHeaders getHeaders() {
        return headers;
    }

    private static long seconds(Duration retryAfter) {
        return Math.max(1, (retryAfter.toMillis() + 999) / 1000);
    }

    private static ProblemDetail problem(long seconds, String reason) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
                reason + " Réessayez dans " + seconds + " s.");
        problem.setType(URI.create("https://hoopmind.dev/problems/too-many-attempts"));
        problem.setTitle("Connexion temporairement bloquée");
        problem.setProperty("code", "TOO_MANY_ATTEMPTS");
        problem.setProperty("retryAfterSeconds", seconds);
        return problem;
    }
}
