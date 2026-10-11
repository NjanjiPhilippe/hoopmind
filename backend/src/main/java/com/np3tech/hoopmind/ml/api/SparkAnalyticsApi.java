package com.np3tech.hoopmind.ml.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * API pour l'analyse avancée et les prédictions avec Spark
 *
 * Ces endpoints exposent les capacités de Machine Learning et d'analytics
 * de Spark pour l'application HoopMind
 */
@Tag(name = "Spark Analytics", description = "Advanced analytics and ML predictions with Apache Spark")
@RequestMapping("/api/v1/spark")
@SecurityRequirement(name = "bearerAuth")
public interface SparkAnalyticsApi {

    // ========== MACHINE LEARNING ENDPOINTS ==========

    @Operation(
            summary = "Train Random Forest model",
            description = "Entraîne un modèle Random Forest pour prédire les résultats des matchs. " +
                    "Utilise 80% des données pour l'entraînement et 20% pour la validation. " +
                    "Retourne les métriques de performance (accuracy, precision, recall, F1, AUC)."
    )
    @PostMapping("/ml/train/random-forest")
    ResponseEntity<Map<String, Object>> trainRandomForestModel(
            @Parameter(description = "ID de la saison pour l'entraînement")
            @RequestParam Long seasonId
    );

    @Operation(
            summary = "Train Logistic Regression model",
            description = "Entraîne un modèle de régression logistique plus simple et rapide. " +
                    "Bon pour comprendre l'importance de chaque statistique."
    )
    @PostMapping("/ml/train/logistic-regression")
    ResponseEntity<Map<String, Object>> trainLogisticRegressionModel(
            @Parameter(description = "ID de la saison")
            @RequestParam Long seasonId
    );

    @Operation(
            summary = "Train Score Prediction model",
            description = "Entraîne un modèle de régression pour prédire les scores des matchs. " +
                    "Utile pour les paris over/under et prédictions de scores exacts."
    )
    @PostMapping("/ml/train/score-prediction")
    ResponseEntity<Map<String, Object>> trainScorePredictionModel(
            @Parameter(description = "ID de la saison")
            @RequestParam Long seasonId
    );

    @Operation(
            summary = "Predict game outcome",
            description = "Utilise le modèle entraîné pour prédire le résultat d'un match. " +
                    "Retourne la prédiction (équipe gagnante) et les probabilités."
    )
    @GetMapping("/ml/predict/game")
    ResponseEntity<Map<String, Object>> predictGameOutcome(
            @Parameter(description = "ID de la saison") @RequestParam Long seasonId,
            @Parameter(description = "ID de l'équipe domicile") @RequestParam Long homeTeamId,
            @Parameter(description = "ID de l'équipe extérieure") @RequestParam Long awayTeamId
    );

    // ========== ANALYTICS ENDPOINTS ==========

    @Operation(
            summary = "Analyze team trends",
            description = "Analyse les tendances des équipes sur la saison: " +
                    "performances moyennes, avantage du terrain, stats domicile vs extérieur. " +
                    "Utilise l'agrégation Spark pour traiter rapidement des milliers de matchs."
    )
    @GetMapping("/analytics/team-trends")
    ResponseEntity<Map<String, Object>> analyzeTeamTrends(
            @Parameter(description = "ID de la saison") @RequestParam Long seasonId
    );

    @Operation(
            summary = "Analyze correlations",
            description = "Calcule les corrélations entre différentes statistiques et les victoires. " +
                    "Identifie quelles stats sont les plus importantes pour gagner. " +
                    "Corrélation: -1 (négative) à 1 (positive), 0 = pas de relation."
    )
    @GetMapping("/analytics/correlations")
    ResponseEntity<Map<String, Double>> analyzeCorrelations(
            @Parameter(description = "ID de la saison") @RequestParam Long seasonId
    );

    @Operation(
            summary = "Analyze performance by period",
            description = "Compare les performances en début, milieu et fin de saison. " +
                    "Identifie les équipes qui progressent ou déclinent. " +
                    "Utile pour ajuster les prédictions selon la forme actuelle."
    )
    @GetMapping("/analytics/performance-by-period")
    ResponseEntity<Map<String, Object>> analyzePerformanceByPeriod(
            @Parameter(description = "ID de la saison") @RequestParam Long seasonId
    );

    @Operation(
            summary = "Analyze injury impact",
            description = "Mesure l'impact des blessures sur les performances des équipes. " +
                    "Compare les taux de victoires avec et sans joueurs clés blessés."
    )
    @GetMapping("/analytics/injury-impact")
    ResponseEntity<Map<String, Object>> analyzeInjuryImpact(
            @Parameter(description = "ID de la saison") @RequestParam Long seasonId
    );

    @Operation(
            summary = "Analyze head-to-head",
            description = "Analyse l'historique des confrontations entre deux équipes. " +
                    "Statistiques des matchups, tendances, avantages spécifiques."
    )
    @GetMapping("/analytics/head-to-head")
    ResponseEntity<Map<String, Object>> analyzeHeadToHead(
            @Parameter(description = "ID de l'équipe 1") @RequestParam Long team1Id,
            @Parameter(description = "ID de l'équipe 2") @RequestParam Long team2Id,
            @Parameter(description = "ID de la saison") @RequestParam Long seasonId
    );

    @Operation(
            summary = "Analyze team momentum",
            description = "Analyse la dynamique actuelle d'une équipe. " +
                    "Forme récente, séries de victoires/défaites, tendances."
    )
    @GetMapping("/analytics/team-momentum")
    ResponseEntity<Map<String, Object>> analyzeTeamMomentum(
            @Parameter(description = "ID de l'équipe") @RequestParam Long teamId,
            @Parameter(description = "ID de la saison") @RequestParam Long seasonId
    );

    @Operation(
            summary = "Generate insights",
            description = "Génère des insights automatiques basés sur les données: " +
                    "facteurs clés de victoire, équipes en forme, prédictibilité de la saison."
    )
    @GetMapping("/analytics/insights")
    ResponseEntity<Map<String, Object>> generateInsights(
            @Parameter(description = "ID de la saison") @RequestParam Long seasonId
    );
}
