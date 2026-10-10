package com.np3tech.hoopmind.nba.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "team_stats",
        uniqueConstraints = @UniqueConstraint(name = "uk_team_season", columnNames = {"team_id", "season_id"}),
        indexes = {
                @Index(name = "idx_team_stats_team", columnList = "team_id"),
                @Index(name = "idx_team_stats_season", columnList = "season_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TeamStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    private Integer wins;
    private Integer losses;

    @Column(name = "win_percentage", precision = 5, scale = 3)
    private BigDecimal winPercentage;

    @Column(name = "conference_rank")
    private Integer conferenceRank;

    @Column(name = "division_rank")
    private Integer divisionRank;

    @Column(name = "home_wins")
    private Integer homeWins;

    @Column(name = "home_losses")
    private Integer homeLosses;

    @Column(name = "away_wins")
    private Integer awayWins;

    @Column(name = "away_losses")
    private Integer awayLosses;

    @Column(name = "points_per_game", precision = 6, scale = 2)
    private BigDecimal pointsPerGame;

    @Column(name = "points_allowed_per_game", precision = 6, scale = 2)
    private BigDecimal pointsAllowedPerGame;

    @Column(name = "points_differential", precision = 6, scale = 2)
    private BigDecimal pointsDifferential;

    @Column(name = "field_goal_percentage", precision = 5, scale = 3)
    private BigDecimal fieldGoalPercentage;

    @Column(name = "three_point_percentage", precision = 5, scale = 3)
    private BigDecimal threePointPercentage;

    @Column(name = "free_throw_percentage", precision = 5, scale = 3)
    private BigDecimal freeThrowPercentage;

    @Column(name = "rebounds_per_game", precision = 5, scale = 2)
    private BigDecimal reboundsPerGame;

    @Column(name = "assists_per_game", precision = 5, scale = 2)
    private BigDecimal assistsPerGame;

    @Column(name = "steals_per_game", precision = 5, scale = 2)
    private BigDecimal stealsPerGame;

    @Column(name = "blocks_per_game", precision = 5, scale = 2)
    private BigDecimal blocksPerGame;

    @Column(name = "turnovers_per_game", precision = 5, scale = 2)
    private BigDecimal turnoversPerGame;

    @Column(name = "offensive_rating", precision = 6, scale = 2)
    private BigDecimal offensiveRating;

    @Column(name = "defensive_rating", precision = 6, scale = 2)
    private BigDecimal defensiveRating;

    @Column(name = "net_rating", precision = 6, scale = 2)
    private BigDecimal netRating;

    @Column(precision = 5, scale = 2)
    private BigDecimal pace;

    @Column(name = "effective_field_goal_percentage", precision = 5, scale = 3)
    private BigDecimal effectiveFieldGoalPercentage;

    @Column(name = "true_shooting_percentage", precision = 5, scale = 3)
    private BigDecimal trueShootingPercentage;

    @Column(name = "current_streak")
    private Integer currentStreak;

    @Column(name = "longest_win_streak")
    private Integer longestWinStreak;

    @Column(name = "longest_lose_streak")
    private Integer longestLoseStreak;

    @Column(name = "last_10_games_json", columnDefinition = "TEXT")
    private String last10GamesJson;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}