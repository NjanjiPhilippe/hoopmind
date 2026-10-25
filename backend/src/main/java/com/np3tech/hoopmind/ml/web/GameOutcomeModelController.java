package com.np3tech.hoopmind.ml.web;

import com.np3tech.hoopmind.ml.GameOutcomeModel;
import com.np3tech.hoopmind.ml.GameOutcomePrediction;
import com.np3tech.hoopmind.ml.GameOutcomePredictor;
import com.np3tech.hoopmind.ml.TrainingReport;
import com.np3tech.hoopmind.ml.training.ModelTrainingJobs;
import com.np3tech.hoopmind.ml.training.ModelTrainingJobs.TrainingJobStatus;
import com.np3tech.hoopmind.shared.exception.AppException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Entraînement, métriques et prédictions du modèle des matchs. Ne dépend que des ports {@link GameOutcomeModel}
 * et {@link GameOutcomePredictor} : sans Spark, l'entraînement répond 503 et les prédictions restent disponibles.
 */
@RestController
@RequestMapping("/api/v1/spark/ml")
@Tag(name = "Spark Analytics")
@SecurityRequirement(name = "bearerAuth")
class GameOutcomeModelController {

    private final ObjectProvider<GameOutcomeModel> model;
    private final ModelTrainingJobs trainings;
    private final GameOutcomePredictor predictor;
    private final Clock clock;

    GameOutcomeModelController(ObjectProvider<GameOutcomeModel> model, ModelTrainingJobs trainings,
                               GameOutcomePredictor predictor, Clock clock) {
        this.model = model;
        this.trainings = trainings;
        this.predictor = predictor;
        this.clock = clock;
    }

    @GetMapping("/predict/game")
    @Operation(operationId = "predictGameOutcome", summary = "Prédire le vainqueur d'un match",
            description = "Forme des deux équipes avant la date du match (matchs déjà joués de la saison, complétés en début de "
                    + "saison par le bilan de la saison passée), puis le modèle "
                    + "entraîné. Calculé en Java : disponible même si Spark est désactivé. 409 si le modèle n'est pas "
                    + "entraîné, 422 si une équipe n'a joué aucun match ni cette saison avant cette date, ni la saison passée.")
    GameOutcomePrediction predictGameOutcome(
            @Parameter(description = "ID de la saison") @RequestParam long seasonId,
            @Parameter(description = "ID de l'équipe à domicile") @RequestParam long homeTeamId,
            @Parameter(description = "ID de l'équipe à l'extérieur") @RequestParam long awayTeamId,
            @Parameter(description = "Jour du match (aujourd'hui par défaut)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Match de playoffs") @RequestParam(defaultValue = "false") boolean playoff) {
        return predictor.predict(seasonId, homeTeamId, awayTeamId, date != null ? date : LocalDate.now(clock), playoff);
    }

    @GetMapping("/model-metrics")
    @Operation(operationId = "getModelMetrics", summary = "Métriques du modèle de prédiction des matchs",
            description = "Dernier entraînement enregistré (404 s'il n'a jamais été entraîné).")
    ResponseEntity<TrainingReport> getModelMetrics() {
        return ResponseEntity.of(model().latestReport());
    }

    @PostMapping("/train/game-outcome/jobs")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(operationId = "startGameOutcomeTraining", summary = "Lancer l'entraînement du modèle en arrière-plan",
            description = "Régression logistique sur la forme des équipes avant chaque match, apprise sur les saisons "
                    + "les plus anciennes et testée sur la plus récente. Suivre l'avancement avec GET /train/game-outcome/status. "
                    + "409 si un import ou un autre entraînement est en cours.")
    TrainingJobStatus startGameOutcomeTraining(
            @Parameter(description = "Nombre de saisons à utiliser, les plus récentes (réglage ML_TRAINING_SEASONS sinon)")
            @RequestParam(required = false) Integer seasons) {
        return trainings.start(seasons);
    }

    @GetMapping("/train/game-outcome/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(operationId = "getGameOutcomeTrainingStatus", summary = "État du dernier entraînement du modèle",
            description = "204 si le modèle n'a jamais été entraîné par un job.")
    ResponseEntity<TrainingJobStatus> getGameOutcomeTrainingStatus() {
        return trainings.latest().map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    private GameOutcomeModel model() {
        GameOutcomeModel available = model.getIfAvailable();
        if (available == null) {
            throw new AppException("Le moteur de prédiction est désactivé (SPARK_ENABLED=false).",
                    HttpStatus.SERVICE_UNAVAILABLE, "MODEL_ENGINE_DISABLED");
        }
        return available;
    }
}
