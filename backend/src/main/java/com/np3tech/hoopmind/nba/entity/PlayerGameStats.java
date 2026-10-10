package com.np3tech.hoopmind.nba.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "player_game_stats",
        uniqueConstraints = @UniqueConstraint(name = "uk_player_game", columnNames = {"player_id", "game_id"}),
        indexes = {
                @Index(name = "idx_player_game_stats_player", columnList = "player_id"),
                @Index(name = "idx_player_game_stats_game", columnList = "game_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlayerGameStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_result_id")
    private GameResult gameResult;

    @Builder.Default
    private Boolean started = false;

    @Column(name = "minutes_played")
    private Integer minutesPlayed;

    private Integer points;

    @Column(name = "field_goals_made")
    private Integer fieldGoalsMade;

    @Column(name = "field_goals_attempted")
    private Integer fieldGoalsAttempted;

    @Column(name = "three_points_made")
    private Integer threePointsMade;

    @Column(name = "three_points_attempted")
    private Integer threePointsAttempted;

    @Column(name = "free_throws_made")
    private Integer freeThrowsMade;

    @Column(name = "free_throws_attempted")
    private Integer freeThrowsAttempted;

    @Column(name = "offensive_rebounds")
    private Integer offensiveRebounds;

    @Column(name = "defensive_rebounds")
    private Integer defensiveRebounds;

    @Column(name = "total_rebounds")
    private Integer totalRebounds;

    private Integer assists;
    private Integer steals;
    private Integer blocks;
    private Integer turnovers;

    @Column(name = "personal_fouls")
    private Integer personalFouls;

    @Column(name = "plus_minus")
    private Integer plusMinus;
}
