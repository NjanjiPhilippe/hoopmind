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

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
