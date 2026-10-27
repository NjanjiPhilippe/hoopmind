package com.np3tech.hoopmind.ml.web;

import com.np3tech.hoopmind.ml.explanation.ExplainedPrediction;
import com.np3tech.hoopmind.ml.explanation.PredictionExplanationService;
import com.np3tech.hoopmind.ml.history.PredictedMatch;
import com.np3tech.hoopmind.ml.history.PredictionHistory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
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
@Slf4j
class PredictionExplanationController {

    private final PredictionExplanationService explanations;
    private final PredictionHistory history;
    private final Clock clock;

    PredictionExplanationController(PredictionExplanationService explanations, PredictionHistory history, Clock clock) {
        this.explanations = explanations;
        this.history = history;
        this.clock = clock;
    }

    @GetMapping("/predict/game/explanation")
    @Operation(operationId = "explainGameOutcome", summary = "Prédire le vainqueur et le score d'un match et expliquer pourquoi",
            description = "Même prédiction que GET /predict/game (score compris), avec le poids de chaque facteur et une explication en "
                    + "français. Le texte est rédigé par le LLM configuré (hoopmind.ml.explanation.provider) à partir des "
                    + "seuls chiffres du modèle ; sans LLM, ou s'il ne répond pas, une explication est rédigée sans lui "
                    + "(fallback = true), de même au-delà de la limite d'appels au LLM par utilisateur et par heure. La prédiction est "
                    + "ajoutée à « Mes prédictions » de l'utilisateur. 409 si le modèle n'est pas entraîné, 422 si une équipe n'a joué aucun "
                    + "match ni cette saison avant cette date, ni la saison passée.")
    ExplainedPrediction explainGameOutcome(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "ID de la saison") @RequestParam long seasonId,
            @Parameter(description = "ID de l'équipe à domicile") @RequestParam long homeTeamId,
            @Parameter(description = "ID de l'équipe à l'extérieur") @RequestParam long awayTeamId,
            @Parameter(description = "Jour du match (aujourd'hui par défaut)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Match de playoffs") @RequestParam(defaultValue = "false") boolean playoff) {
        LocalDate day = date != null ? date : LocalDate.now(clock);
        ExplainedPrediction explained = explanations.explain(jwt.getSubject(), seasonId, homeTeamId, awayTeamId, day, playoff);
        try {
            history.record(Long.parseLong(jwt.getSubject()), new PredictedMatch(seasonId, homeTeamId, awayTeamId, day, playoff),
                    explained.prediction());
        } catch (DataAccessException e) {
            // L'historique ne doit jamais faire perdre la prédiction (deux demandes simultanées du même match…).
            log.warn("Prédiction non enregistrée dans l'historique de l'utilisateur {} : {}", jwt.getSubject(), e.getMessage());
        }
        return explained;
    }
}
