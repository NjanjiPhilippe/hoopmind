package com.np3tech.hoopmind.ml.api;

import com.np3tech.hoopmind.ml.dto.request.MatchPredictionCreateRequest;
import com.np3tech.hoopmind.ml.dto.response.MatchPredictionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "Predictions", description = "Prediction APIs")
@RequestMapping("/api/v1/predictions")
@SecurityRequirement(name = "bearerAuth")
public interface PredictionApi {

    @Operation(summary = "Create a prediction")
    @PostMapping
    ResponseEntity<MatchPredictionResponse> createPrediction(
            @Valid @RequestBody MatchPredictionCreateRequest request
    );

    @Operation(summary = "Get prediction by ID")
    @GetMapping("/{id}")
    ResponseEntity<MatchPredictionResponse> getPredictionById(@PathVariable Long id);

    @Operation(summary = "Get prediction by game")
    @GetMapping("/game/{gameId}")
    ResponseEntity<MatchPredictionResponse> getPredictionByGame(@PathVariable Long gameId);

    @Operation(summary = "Get predictions by model")
    @GetMapping("/model/{model}")
    ResponseEntity<List<MatchPredictionResponse>> getPredictionsByModel(@PathVariable String model);

    @Operation(summary = "Get evaluated predictions")
    @GetMapping("/evaluated")
    ResponseEntity<List<MatchPredictionResponse>> getEvaluatedPredictions();

    @Operation(summary = "Evaluate prediction (Admin only)")
    @PostMapping("/{id}/evaluate")
    ResponseEntity<MatchPredictionResponse> evaluatePrediction(@PathVariable Long id);

    @Operation(summary = "Get prediction accuracy report")
    @GetMapping("/accuracy/model/{model}")
    ResponseEntity<?> getPredictionAccuracyReport(@PathVariable String model);
}
