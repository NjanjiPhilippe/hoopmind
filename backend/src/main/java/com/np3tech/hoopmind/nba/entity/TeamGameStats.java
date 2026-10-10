package com.np3tech.hoopmind.nba.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "team_game_stats", indexes = {
        @Index(name = "idx_team_game_stats_team", columnList = "team_id"),
        @Index(name = "idx_team_game_stats_game", columnList = "game_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TeamGameStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_result_id")
    private GameResult gameResult;

    @Column(nullable = false)
    private Integer points;

    @Column(name = "field_goals_made")
    private Integer fieldGoalsMade;

    @Column(name = "field_goals_attempted")
    private Integer fieldGoalsAttempted;

    @Column(name = "field_goal_percentage", precision = 5, scale = 3)
    private BigDecimal fieldGoalPercentage;

    @Column(name = "three_points_made")
    private Integer threePointsMade;

    @Column(name = "three_points_attempted")
    private Integer threePointsAttempted;

    @Column(name = "three_point_percentage", precision = 5, scale = 3)
    private BigDecimal threePointPercentage;

    @Column(name = "free_throws_made")
    private Integer freeThrowsMade;

    @Column(name = "free_throws_attempted")
    private Integer freeThrowsAttempted;

    @Column(name = "free_throw_percentage", precision = 5, scale = 3)
    private BigDecimal freeThrowPercentage;

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

    @Column(name = "fast_break_points")
    private Integer fastBreakPoints;

    @Column(name = "points_in_paint")
    private Integer pointsInPaint;

    @Column(name = "second_chance_points")
    private Integer secondChancePoints;

    @Column(name = "bench_points")
    private Integer benchPoints;

    @Column(name = "biggest_lead")
    private Integer biggestLead;

    @Column(name = "time_of_possession", precision = 5, scale = 2)
    private BigDecimal timeOfPossession;
}