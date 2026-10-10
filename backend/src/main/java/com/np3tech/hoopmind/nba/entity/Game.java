package com.np3tech.hoopmind.nba.entity;

import com.np3tech.hoopmind.nba.entity.enummeration.GameStatus;
import com.np3tech.hoopmind.nba.entity.enummeration.GameType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "games", indexes = {
        @Index(name = "idx_game_home_team", columnList = "home_team_id"),
        @Index(name = "idx_game_away_team", columnList = "away_team_id"),
        @Index(name = "idx_game_date", columnList = "scheduled_date"),
        @Index(name = "idx_game_status", columnList = "status"),
        @Index(name = "idx_game_season", columnList = "season_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team_id", nullable = false)
    private Team homeTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_team_id", nullable = false)
    private Team awayTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_type", nullable = false, length = 30)
    private GameType gameType;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDateTime scheduledDate;

    @Column(name = "actual_start_time")
    private LocalDateTime actualStartTime;

    @Column(name = "actual_end_time")
    private LocalDateTime actualEndTime;

    @Column(length = 200)
    private String venue;

    private Integer attendance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private GameStatus status = GameStatus.SCHEDULED;

    @Column(name = "referees_json", columnDefinition = "TEXT")
    private String refereesJson;

    @Column(name = "is_national_tv")
    @Builder.Default
    private Boolean isNationalTv = false;

    @Column(name = "broadcast_network", length = 100)
    private String broadcastNetwork;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}