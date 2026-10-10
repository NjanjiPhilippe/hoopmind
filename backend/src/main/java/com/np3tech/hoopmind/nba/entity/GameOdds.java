package com.np3tech.hoopmind.nba.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_odds", indexes = {
        @Index(name = "idx_odds_game", columnList = "game_id"),
        @Index(name = "idx_odds_bookmaker", columnList = "bookmaker"),
        @Index(name = "idx_odds_created", columnList = "created_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GameOdds {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(nullable = false, length = 100)
    private String bookmaker;

    @Column(name = "home_team_moneyline", nullable = false, precision = 5, scale = 2)
    private BigDecimal homeTeamMoneyline;

    @Column(name = "away_team_moneyline", nullable = false, precision = 5, scale = 2)
    private BigDecimal awayTeamMoneyline;

    @Column(precision = 5, scale = 2)
    private BigDecimal spread;

    @Column(name = "home_spread_odds", precision = 5, scale = 2)
    private BigDecimal homeSpreadOdds;

    @Column(name = "away_spread_odds", precision = 5, scale = 2)
    private BigDecimal awaySpreadOdds;

    @Column(name = "over_under", precision = 6, scale = 2)
    private BigDecimal overUnder;

    @Column(name = "over_odds", precision = 5, scale = 2)
    private BigDecimal overOdds;

    @Column(name = "under_odds", precision = 5, scale = 2)
    private BigDecimal underOdds;

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
