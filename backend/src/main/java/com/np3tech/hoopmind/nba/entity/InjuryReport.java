package com.np3tech.hoopmind.nba.entity;

import com.np3tech.hoopmind.nba.entity.enummeration.InjuryStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "injury_reports", indexes = {
        @Index(name = "idx_injury_player", columnList = "player_id"),
        @Index(name = "idx_injury_team", columnList = "team_id"),
        @Index(name = "idx_injury_status", columnList = "status"),
        @Index(name = "idx_injury_date", columnList = "report_date")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InjuryReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "injury_type", nullable = false, length = 255)
    private String injuryType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InjuryStatus status;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "expected_return_date")
    private LocalDate expectedReturnDate;

    @Column(name = "actual_return_date")
    private LocalDate actualReturnDate;

    @Column(name = "games_expected_to_miss")
    private Integer gamesExpectedToMiss;

    @Column(name = "games_actually_missed")
    private Integer gamesActuallyMissed;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

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
