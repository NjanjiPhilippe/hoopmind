package com.np3tech.hoopmind.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import java.net.URI;

/**
 * Erreurs métier du module, rendues au format RFC 9457 (problem+json).
 */
public class UserException extends ErrorResponseException {

    private UserException(HttpStatusCode status, String type, String title, String detail) {
        super(status, problem(status, type, title, detail), null);
    }

    private static ProblemDetail problem(HttpStatusCode status, String type, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("https://hoopmind.dev/problems/" + type));
        problem.setTitle(title);
        return problem;
    }

    public static UserException notFound(Long id) {
        return new UserException(HttpStatus.NOT_FOUND, "user-not-found", "Utilisateur introuvable",
                "Aucun utilisateur avec l'identifiant " + id);
    }

    public static UserException usernameTaken() {
        return new UserException(HttpStatus.CONFLICT, "username-taken", "Nom d'utilisateur déjà pris",
                "Ce nom d'utilisateur est déjà utilisé");
    }

    public static UserException emailTaken() {
        return new UserException(HttpStatus.CONFLICT, "email-taken", "Adresse e-mail déjà utilisée",
                "Un compte existe déjà avec cette adresse e-mail");
    }

    public static UserException wrongPassword() {
        return new UserException(HttpStatus.BAD_REQUEST, "wrong-password", "Mot de passe incorrect",
                "Le mot de passe actuel est incorrect");
    }

    public static UserException lastAdmin() {
        return new UserException(HttpStatus.CONFLICT, "last-admin", "Dernier administrateur",
                "Cette action retirerait le dernier administrateur actif");
    }
}
