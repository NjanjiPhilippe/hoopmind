package com.np3tech.hoopmind.nba.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "player_stats",
        uniqueConstraints = @UniqueConstraint(name = "uk_player_season", columnNames = {"player_id", "season_id"}),
        indexes = {
                @Index(name = "idx_player_stats_player", columnList = "player_id"),
                @Index(name = "idx_player_stats_season", columnList = "season_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlayerStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Column(name = "games_played")
    private Integer gamesPlayed;

    @Column(name = "games_started")
    private Integer gamesStarted;

    @Column(name = "minutes_per_game", precision = 5, scale = 2)
    private BigDecimal minutesPerGame;

    @Column(name = "points_per_game", precision = 5, scale = 2)
    private BigDecimal pointsPerGame;

    @Column(name = "field_goals_made", precision = 5, scale = 2)
    private BigDecimal fieldGoalsMade;

    @Column(name = "field_goals_attempted", precision = 5, scale = 2)
    private BigDecimal fieldGoalsAttempted;

    @Column(name = "field_goal_percentage", precision = 5, scale = 3)
    private BigDecimal fieldGoalPercentage;

    @Column(name = "three_points_made", precision = 5, scale = 2)
    private BigDecimal threePointsMade;

    @Column(name = "three_points_attempted", precision = 5, scale = 2)
    private BigDecimal threePointsAttempted;

    @Column(name = "three_point_percentage", precision = 5, scale = 3)
    private BigDecimal threePointPercentage;

    @Column(name = "free_throws_made", precision = 5, scale = 2)
    private BigDecimal freeThrowsMade;

    @Column(name = "free_throws_attempted", precision = 5, scale = 2)
    private BigDecimal freeThrowsAttempted;

    @Column(name = "free_throw_percentage", precision = 5, scale = 3)
    private BigDecimal freeThrowPercentage;

    @Column(name = "offensive_rebounds", precision = 5, scale = 2)
    private BigDecimal offensiveRebounds;

    @Column(name = "defensive_rebounds", precision = 5, scale = 2)
    private BigDecimal defensiveRebounds;

    @Column(name = "total_rebounds_per_game", precision = 5, scale = 2)
    private BigDecimal totalReboundsPerGame;

    @Column(name = "assists_per_game", precision = 5, scale = 2)
    private BigDecimal assistsPerGame;

    @Column(name = "steals_per_game", precision = 5, scale = 2)
    private BigDecimal stealsPerGame;

    @Column(name = "blocks_per_game", precision = 5, scale = 2)
    private BigDecimal blocksPerGame;

    @Column(name = "turnovers_per_game", precision = 5, scale = 2)
    private BigDecimal turnoversPerGame;

    @Column(name = "fouls_per_game", precision = 5, scale = 2)
    private BigDecimal foulsPerGame;

    @Column(name = "player_efficiency_rating", precision = 5, scale = 2)
    private BigDecimal playerEfficiencyRating;

    @Column(name = "true_shooting_percentage", precision = 5, scale = 3)
    private BigDecimal trueShootingPercentage;

    @Column(name = "effective_field_goal_percentage", precision = 5, scale = 3)
    private BigDecimal effectiveFieldGoalPercentage;

    @Column(name = "plus_minus", precision = 6, scale = 2)
    private BigDecimal plusMinus;

    @Column(name = "offensive_rating", precision = 6, scale = 2)
    private BigDecimal offensiveRating;

    @Column(name = "defensive_rating", precision = 6, scale = 2)
    private BigDecimal defensiveRating;

    @Column(name = "usage_rate", precision = 5, scale = 3)
    private BigDecimal usageRate;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}