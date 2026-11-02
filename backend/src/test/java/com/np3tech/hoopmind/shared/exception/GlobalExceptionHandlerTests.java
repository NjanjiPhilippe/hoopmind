package com.np3tech.hoopmind.shared.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ADR 0008 : une erreur inattendue ne révèle ni chemin, ni requête SQL, ni message technique. Si le filet de
 * {@link GlobalExceptionHandler} se met à recopier le message de l'exception, ces tests échouent.
 */
class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void anUnexpectedErrorIsAGeneric500() {
        ProblemDetail problem = handler.handleUnexpected(
                new IllegalStateException("/srv/hoopmind/data/nba.sqlite: SELECT * FROM users failed"));

        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getType().toString()).endsWith("/internal-error");
        assertThat(problem.getDetail()).isEqualTo("Une erreur inattendue est survenue")
                .doesNotContain("/srv", "SELECT");
    }

    @Test
    void anIntegrityViolationIsAGeneric409() {
        ProblemDetail problem = handler.handleDataIntegrity(new DataIntegrityViolationException("refusé",
                new SQLException("duplicate key value violates unique constraint \"users_email_key\"")));

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getDetail()).doesNotContain("users_email_key", "duplicate key");
    }
}
