package com.np3tech.hoopmind.ml.history;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Une prédiction demandée par un utilisateur. Les équipes et la saison ne sont que des identifiants : le module ML ne
 * connaît pas les entités NBA ({@code GameHistory} donne les noms et les résultats).
 */
@Entity
@Table(name = "prediction_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class SavedPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private long userId;

    @Column(name = "season_id", nullable = false, updatable = false)
    private long seasonId;

    @Column(name = "home_team_id", nullable = false, updatable = false)
    private long homeTeamId;

    @Column(name = "away_team_id", nullable = false, updatable = false)
    private long awayTeamId;

    @Column(name = "game_date", nullable = false, updatable = false)
    private LocalDate gameDate;

    @Column(nullable = false, updatable = false)
    private boolean playoff;

    @Column(name = "home_win_probability", nullable = false)
    private double homeWinProbability;

    @Column(name = "predicted_home_points")
    private Integer predictedHomePoints;

    @Column(name = "predicted_away_points")
    private Integer predictedAwayPoints;

    @Column(nullable = false, length = 60)
    private String model;

    @Column(name = "predicted_at", nullable = false)
    private Instant predictedAt;

    SavedPrediction(long userId, PredictedMatch match) {
        this.userId = userId;
        this.seasonId = match.seasonId();
        this.homeTeamId = match.homeTeamId();
        this.awayTeamId = match.awayTeamId();
        this.gameDate = match.date();
        this.playoff = match.playoff();
    }

    /** Une nouvelle demande du même match remplace la précédente (le modèle ou la forme ont pu changer). */
    void update(double homeWinProbability, Integer homePoints, Integer awayPoints, String model, Instant at) {
        this.homeWinProbability = homeWinProbability;
        this.predictedHomePoints = homePoints;
        this.predictedAwayPoints = awayPoints;
        this.model = model;
        this.predictedAt = at;
    }
}
