package com.np3tech.hoopmind.ml.entity;

import com.np3tech.hoopmind.user.domain.User;
import com.np3tech.hoopmind.nba.entity.*;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_predictions",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_game", columnNames = {"user_id", "game_id"}),
        indexes = {
                @Index(name = "idx_user_pred_user", columnList = "user_id"),
                @Index(name = "idx_user_pred_game", columnList = "game_id"),
                @Index(name = "idx_user_pred_correct", columnList = "is_correct")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserPrediction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "predicted_winner_id", nullable = false)
    private Team predictedWinner;

    @Column(name = "predicted_home_score")
    private Integer predictedHomeScore;

    @Column(name = "predicted_away_score")
    private Integer predictedAwayScore;

    @Column(length = 1000)
    private String reasoning;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    private Integer points;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
