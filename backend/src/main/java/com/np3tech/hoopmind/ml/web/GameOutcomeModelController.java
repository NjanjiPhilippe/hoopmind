package com.np3tech.hoopmind.ml.web;

import com.np3tech.hoopmind.ml.GameOutcomeModel;
import com.np3tech.hoopmind.ml.TrainingReport;
import com.np3tech.hoopmind.ml.training.ModelTrainingJobs;
import com.np3tech.hoopmind.ml.training.ModelTrainingJobs.TrainingJobStatus;
import com.np3tech.hoopmind.shared.exception.AppException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Entraînement et métriques du modèle de prédiction des matchs. Ne dépend que du port {@link GameOutcomeModel} :
 * sans moteur de calcul (Spark désactivé), les routes répondent 503 au lieu de disparaître.
 */
@RestController
@RequestMapping("/api/v1/spark/ml")
@Tag(name = "Spark Analytics")
@SecurityRequirement(name = "bearerAuth")
class GameOutcomeModelController {

    private final ObjectProvider<GameOutcomeModel> model;
    private final ModelTrainingJobs trainings;

    GameOutcomeModelController(ObjectProvider<GameOutcomeModel> model, ModelTrainingJobs trainings) {
        this.model = model;
        this.trainings = trainings;
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

    @Deprecated
    @PostMapping("/train/game-outcome")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(operationId = "trainGameOutcomeModel", deprecated = true,
            summary = "Entraîner le modèle et attendre le résultat (remplacé par POST /train/game-outcome/jobs)",
            description = "Lance le même job que POST /train/game-outcome/jobs et attend sa fin.")
    TrainingReport trainGameOutcomeModel(
            @Parameter(description = "Nombre de saisons à utiliser, les plus récentes (réglage ML_TRAINING_SEASONS sinon)")
            @RequestParam(required = false) Integer seasons) {
        return trainings.trainAndWait(seasons);
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
