package com.np3tech.hoopmind.ml.scoring;

import com.np3tech.hoopmind.ml.GameOutcomePrediction;
import com.np3tech.hoopmind.ml.GameOutcomePredictor;
import com.np3tech.hoopmind.nba.GameHistory;
import com.np3tech.hoopmind.nba.GameRecord;
import com.np3tech.hoopmind.shared.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Prédit un match en Java pur, en quelques millisecondes : forme des deux équipes à la date du match (même calcul
 * qu'à l'entraînement), puis la régression logistique entraînée par Spark, appliquée avec sa carte de score.
 * Spark n'est nécessaire que pour entraîner ; les prédictions fonctionnent même s'il est désactivé.
 */
@Component
class ScoringGameOutcomePredictor implements GameOutcomePredictor {

    private final GameHistory history;
    private final ScoringCardStore cards;

    ScoringGameOutcomePredictor(GameHistory history, ScoringCardStore cards) {
        this.history = history;
        this.cards = cards;
    }

    @Override
    public GameOutcomePrediction predict(long seasonId, long homeTeamId, long awayTeamId, LocalDate date, boolean playoff) {
        ScoringCard card = cards.current().orElseThrow(() -> new AppException(
                "Le modèle n'est pas encore entraîné (POST /api/v1/spark/ml/train/game-outcome/jobs).",
                HttpStatus.CONFLICT, "MODEL_NOT_TRAINED"));
        int seasonYear = history.seasonYear(seasonId).orElseThrow(() -> new AppException(
                "Saison introuvable : " + seasonId, HttpStatus.NOT_FOUND, "SEASON_NOT_FOUND"));
        List<GameRecord> games = history.teamGames(seasonYear, List.of(homeTeamId, awayTeamId));

        TeamForm home = TeamForm.before(games, homeTeamId, date, null);
        TeamForm away = TeamForm.before(games, awayTeamId, date, null);
        if (home.gamesPlayed() == 0 || away.gamesPlayed() == 0) {
            throw new AppException("Ces équipes n'ont pas encore joué de match dans cette saison avant cette date.",
                    HttpStatus.UNPROCESSABLE_CONTENT, "NO_TEAM_FORM");
        }
        double probability = card.homeWinProbability(GameOutcomeFeatures.of(home, away, playoff));
        return new GameOutcomePrediction(probability >= 0.5, probability, 1 - probability, card.model());
    }
}
