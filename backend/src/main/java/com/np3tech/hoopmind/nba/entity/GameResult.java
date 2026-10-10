package com.np3tech.hoopmind.nba.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_results", indexes = {
        @Index(name = "idx_game_result_game", columnList = "game_id"),
        @Index(name = "idx_game_result_winner", columnList = "winner_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GameResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false, unique = true)
    private Game game;

    @Column(name = "home_team_score", nullable = false)
    private Integer homeTeamScore;

    @Column(name = "away_team_score", nullable = false)
    private Integer awayTeamScore;

    @Column(name = "score_difference", nullable = false)
    private Integer scoreDifference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id", nullable = false)
    private Team winner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loser_id", nullable = false)
    private Team loser;

    @Column(name = "home_team_quarter_scores_json", columnDefinition = "TEXT")
    private String homeTeamQuarterScoresJson;

    @Column(name = "away_team_quarter_scores_json", columnDefinition = "TEXT")
    private String awayTeamQuarterScoresJson;

    @Builder.Default
    private Boolean overtime = false;

    @Column(name = "overtime_periods")
    private Integer overtimePeriods;

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;
}
