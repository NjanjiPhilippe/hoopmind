package com.np3tech.hoopmind.ml.controller;

import com.np3tech.hoopmind.ml.GameOutcomePrediction;
import com.np3tech.hoopmind.ml.GameOutcomePredictor;
import com.np3tech.hoopmind.ml.api.SparkAnalyticsApi;
import com.np3tech.hoopmind.ml.spark.ConditionalOnSpark;
import com.np3tech.hoopmind.ml.spark.SparkAnalyticsService;
import com.np3tech.hoopmind.ml.spark.SparkMLPredictionService;
import com.np3tech.hoopmind.shared.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur pour les fonctionnalités Spark
 *
 * SÉCURITÉ:
 * - Les endpoints d'entraînement sont réservés aux ADMIN
 * - Les analytics sont accessibles à tous les utilisateurs authentifiés
 *
 * PERFORMANCE:
 * - Les opérations Spark peuvent prendre plusieurs secondes
 * - Considérer l'ajout de cache pour les résultats fréquents
 * - Possibilité d'exécution asynchrone pour les longues analyses
 */
@RestController
@ConditionalOnSpark
@RequiredArgsConstructor
@Slf4j
public class SparkAnalyticsController implements SparkAnalyticsApi {

    private final SparkMLPredictionService mlService;
    private final SparkAnalyticsService analyticsService;
    private final GameOutcomePredictor predictor;

    // ========== MACHINE LEARNING ENDPOINTS ==========

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> trainRandomForestModel(Long seasonId) {
        log.info("Training Random Forest model for season: {}", seasonId);

        try {
            Map<String, Object> metrics = mlService.trainRandomForestClassifier(seasonId);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Model trained successfully");
            response.put("metrics", metrics);
            response.put("seasonId", seasonId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error training Random Forest model", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to train model: " + e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> trainLogisticRegressionModel(Long seasonId) {
        log.info("Training Logistic Regression model for season: {}", seasonId);

        try {
            Map<String, Object> metrics = mlService.trainLogisticRegressionModel(seasonId);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Logistic Regression model trained successfully");
            response.put("metrics", metrics);
            response.put("seasonId", seasonId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error training Logistic Regression model", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> trainScorePredictionModel(Long seasonId) {
        log.info("Training Score Prediction model for season: {}", seasonId);

        try {
            Map<String, Object> metrics = mlService.trainScorePredictionModel(seasonId);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Score Prediction model trained successfully");
            response.put("metrics", metrics);
            response.put("seasonId", seasonId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error training Score Prediction model", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @Override
    public ResponseEntity<Map<String, Object>> predictGameOutcome(
            Long seasonId, Long homeTeamId, Long awayTeamId) {

        log.info("Predicting game outcome: Season {}, Home Team {} vs Away Team {}",
                seasonId, homeTeamId, awayTeamId);

        try {
            GameOutcomePrediction prediction = predictor.predict(seasonId, homeTeamId, awayTeamId);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("prediction", prediction);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error predicting game outcome", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Prediction failed: " + e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // ========== ANALYTICS ENDPOINTS ==========

    @Override
    public ResponseEntity<Map<String, Object>> analyzeTeamTrends(Long seasonId) {
        log.info("Analyzing team trends for season: {}", seasonId);

        try {
            Map<String, Object> trends = analyticsService.analyzeTeamTrends(seasonId);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", trends);
            response.put("seasonId", seasonId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error analyzing team trends", e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e));
        }
    }

    @Override
    public ResponseEntity<Map<String, Double>> analyzeCorrelations(Long seasonId) {
        log.info("Analyzing correlations for season: {}", seasonId);

        try {
            Map<String, Double> correlations = analyticsService.analyzeCorrelations(seasonId);
            return ResponseEntity.ok(correlations);

        } catch (Exception e) {
            log.error("Error analyzing correlations", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<Map<String, Object>> analyzePerformanceByPeriod(Long seasonId) {
        log.info("Analyzing performance by period for season: {}", seasonId);

        try {
            Map<String, Object> periodAnalysis =
                    analyticsService.analyzePerformanceByPeriod(seasonId);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", periodAnalysis);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error analyzing performance by period", e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e));
        }
    }

    @Override
    public ResponseEntity<Map<String, Object>> analyzeInjuryImpact(Long seasonId) {
        log.info("Analyzing injury impact for season: {}", seasonId);

        try {
            Map<String, Object> injuryImpact =
                    analyticsService.analyzeInjuryImpact(seasonId);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", injuryImpact);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error analyzing injury impact", e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e));
        }
    }

    @Override
    public ResponseEntity<Map<String, Object>> analyzeHeadToHead(
            Long team1Id, Long team2Id, Long seasonId) {

        log.info("Analyzing head-to-head: Team {} vs Team {} in season {}",
                team1Id, team2Id, seasonId);

        try {
            Map<String, Object> h2hAnalysis = analyticsService.analyzeHeadToHead(
                    team1Id, team2Id, seasonId
            );

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", h2hAnalysis);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error analyzing head-to-head", e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e));
        }
    }

    @Override
    public ResponseEntity<Map<String, Object>> analyzeTeamMomentum(
            Long teamId, Long seasonId) {

        log.info("Analyzing momentum for team {} in season {}", teamId, seasonId);

        try {
            Map<String, Object> momentum = analyticsService.analyzeTeamMomentum(
                    teamId, seasonId
            );

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", momentum);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error analyzing team momentum", e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e));
        }
    }

    @Override
    public ResponseEntity<Map<String, Object>> generateInsights(Long seasonId) {
        log.info("Generating insights for season: {}", seasonId);

        try {
            Map<String, Object> insights = analyticsService.generateInsights(seasonId);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("insights", insights);
            response.put("seasonId", seasonId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error generating insights", e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e));
        }
    }

    // ========== HELPER METHODS ==========

    private Map<String, Object> createErrorResponse(Exception e) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("message", e.getMessage());
        errorResponse.put("timestamp", System.currentTimeMillis());
        return errorResponse;
    }
}
