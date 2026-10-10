package com.np3tech.hoopmind.ml.entity;

import com.np3tech.hoopmind.user.domain.User;
import com.np3tech.hoopmind.nba.entity.*;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "leaderboards",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_season_leaderboard", columnNames = {"user_id", "season_id"}),
        indexes = {
                @Index(name = "idx_leaderboard_user", columnList = "user_id"),
                @Index(name = "idx_leaderboard_season", columnList = "season_id"),
                @Index(name = "idx_leaderboard_points", columnList = "total_points")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Leaderboard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Column(name = "total_predictions", nullable = false)
    @Builder.Default
    private Integer totalPredictions = 0;

    @Column(name = "correct_predictions", nullable = false)
    @Builder.Default
    private Integer correctPredictions = 0;

    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal accuracy = BigDecimal.ZERO;

    @Column(name = "total_points", nullable = false)
    @Builder.Default
    private Integer totalPoints = 0;

    @Column(name = "rank")
    private Integer rank;

    @Column(name = "current_streak")
    private Integer currentStreak;

    @Column(name = "longest_streak")
    private Integer longestStreak;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}
