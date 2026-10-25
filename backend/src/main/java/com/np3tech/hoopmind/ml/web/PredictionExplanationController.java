package com.np3tech.hoopmind.ml.web;

import com.np3tech.hoopmind.ml.explanation.ExplainedPrediction;
import com.np3tech.hoopmind.ml.explanation.PredictionExplanationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;

/** Prédiction d'un match expliquée en français, par un LLM s'il est configuré. */
@RestController
@RequestMapping("/api/v1/spark/ml")
@Tag(name = "Spark Analytics")
@SecurityRequirement(name = "bearerAuth")
class PredictionExplanationController {

    private final PredictionExplanationService explanations;
    private final Clock clock;

    PredictionExplanationController(PredictionExplanationService explanations, Clock clock) {
        this.explanations = explanations;
        this.clock = clock;
    }

    @GetMapping("/predict/game/explanation")
    @Operation(operationId = "explainGameOutcome", summary = "Prédire le vainqueur d'un match et expliquer pourquoi",
            description = "Même prédiction que GET /predict/game, avec le poids de chaque facteur et une explication en "
                    + "français. Le texte est rédigé par le LLM configuré (hoopmind.ml.explanation.provider) à partir des "
                    + "seuls chiffres du modèle ; sans LLM, ou s'il ne répond pas, une explication est rédigée sans lui "
                    + "(fallback = true), de même au-delà de la limite d'appels au LLM par utilisateur et par heure. 409 si le modèle n'est pas entraîné, 422 si une équipe n'a encore joué aucun "
                    + "match avant cette date.")
    ExplainedPrediction explainGameOutcome(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "ID de la saison") @RequestParam long seasonId,
            @Parameter(description = "ID de l'équipe à domicile") @RequestParam long homeTeamId,
            @Parameter(description = "ID de l'équipe à l'extérieur") @RequestParam long awayTeamId,
            @Parameter(description = "Jour du match (aujourd'hui par défaut)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Match de playoffs") @RequestParam(defaultValue = "false") boolean playoff) {
        return explanations.explain(jwt.getSubject(), seasonId, homeTeamId, awayTeamId, date != null ? date : LocalDate.now(clock), playoff);
    }
}
