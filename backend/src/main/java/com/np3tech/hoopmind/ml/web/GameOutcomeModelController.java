package com.np3tech.hoopmind.ml.web;

import com.np3tech.hoopmind.ml.GameOutcomeModel;
import com.np3tech.hoopmind.ml.TrainingReport;
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

    GameOutcomeModelController(ObjectProvider<GameOutcomeModel> model) {
        this.model = model;
    }

    @GetMapping("/model-metrics")
    @Operation(operationId = "getModelMetrics", summary = "Métriques du modèle de prédiction des matchs",
            description = "Dernier entraînement enregistré (404 s'il n'a jamais été entraîné).")
    ResponseEntity<TrainingReport> getModelMetrics() {
        return ResponseEntity.of(model().latestReport());
    }

    @PostMapping("/train/game-outcome")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(operationId = "trainGameOutcomeModel", summary = "Entraîner le modèle de prédiction des matchs",
            description = "Régression logistique sur la forme des équipes avant chaque match, apprise sur les saisons "
                    + "les plus anciennes et testée sur la plus récente, puis comparée à deux règles simples.")
    TrainingReport trainGameOutcomeModel(
            @Parameter(description = "Nombre de saisons à utiliser, les plus récentes (réglage ML_TRAINING_SEASONS sinon)")
            @RequestParam(required = false) Integer seasons) {
        return model().train(seasons);
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
