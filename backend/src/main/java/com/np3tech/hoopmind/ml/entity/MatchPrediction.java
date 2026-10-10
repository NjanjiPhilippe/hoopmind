package com.np3tech.hoopmind.ml.entity;

import com.np3tech.hoopmind.user.domain.User;
import com.np3tech.hoopmind.nba.entity.*;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "match_predictions", indexes = {
        @Index(name = "idx_prediction_game", columnList = "game_id"),
        @Index(name = "idx_prediction_date", columnList = "prediction_date"),
        @Index(name = "idx_prediction_accuracy", columnList = "is_accurate"),
        @Index(name = "idx_prediction_created_by", columnList = "created_by_user_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MatchPrediction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "predicted_winner_id", nullable = false)
    private Team predictedWinner;

    @Column(name = "win_probability", nullable = false, precision = 5, scale = 4)
    private BigDecimal winProbability;

    @Column(name = "predicted_home_score")
    private Integer predictedHomeScore;

    @Column(name = "predicted_away_score")
    private Integer predictedAwayScore;

    @Column(name = "predicted_score_difference")
    private Integer predictedScoreDifference;

    @Column(nullable = false, length = 50)
    private String model;

    @Column(name = "model_version", length = 20)
    private String modelVersion;

    @Column(name = "prediction_factors_json", columnDefinition = "TEXT")
    private String predictionFactorsJson;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "prediction_date", nullable = false)
    private LocalDateTime predictionDate;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "is_accurate")
    private Boolean isAccurate;

    @Column(name = "predicted_winner_correct")
    private Boolean predictedWinnerCorrect;

    @Column(name = "actual_score_difference")
    private Integer actualScoreDifference;

    @Column(name = "prediction_error", precision = 6, scale = 2)
    private BigDecimal predictionError;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    @Column(name = "evaluated_at")
    private LocalDateTime evaluatedAt;
}
