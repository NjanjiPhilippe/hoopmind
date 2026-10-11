package com.np3tech.hoopmind.ml.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "prediction_model_metrics", indexes = {
        @Index(name = "idx_model_metrics_model", columnList = "model"),
        @Index(name = "idx_model_metrics_date", columnList = "evaluation_date")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PredictionModelMetrics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String model;

    @Column(name = "model_version", nullable = false, length = 20)
    private String modelVersion;

    @Column(name = "evaluation_date", nullable = false)
    private LocalDate evaluationDate;

    @Column(name = "total_predictions", nullable = false)
    private Integer totalPredictions;

    @Column(name = "correct_predictions", nullable = false)
    private Integer correctPredictions;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal accuracy;

    @Column(name = "precision", precision = 5, scale = 4)
    private BigDecimal precisionScore;

    @Column(precision = 5, scale = 4)
    private BigDecimal recall;

    @Column(name = "f1_score", precision = 5, scale = 4)
    private BigDecimal f1Score;

    @Column(name = "mean_absolute_error", precision = 6, scale = 2)
    private BigDecimal meanAbsoluteError;

    @Column(name = "root_mean_squared_error", precision = 6, scale = 2)
    private BigDecimal rootMeanSquaredError;

    @Column(name = "confidence_score", precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    @Column(name = "confusion_matrix_json", columnDefinition = "TEXT")
    private String confusionMatrixJson;

    @Column(name = "feature_importance_json", columnDefinition = "TEXT")
    private String featureImportanceJson;

    /** Nombre de saisons utilisées pour l'entraînement et la saison gardée pour le test. */
    @Column(name = "training_seasons")
    private Integer trainingSeasons;

    @Column(name = "test_season", length = 20)
    private String testSeason;

    @Column(precision = 5, scale = 4)
    private BigDecimal auc;

    @Column(name = "log_loss", precision = 6, scale = 4)
    private BigDecimal logLoss;

    /**
     * Précision de règles simples sur le même jeu de test, pour juger le gain réel du modèle :
     * « l'équipe au meilleur bilan gagne » et « l'équipe à domicile gagne ».
     */
    @Column(name = "baseline_accuracy", precision = 5, scale = 4)
    private BigDecimal baselineAccuracy;

    @Column(name = "home_baseline_accuracy", precision = 5, scale = 4)
    private BigDecimal homeBaselineAccuracy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
