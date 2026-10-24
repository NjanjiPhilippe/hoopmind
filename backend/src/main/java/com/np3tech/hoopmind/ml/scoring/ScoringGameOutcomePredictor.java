package com.np3tech.hoopmind.ml.scoring;

import com.np3tech.hoopmind.ml.GameOutcomeAnalysis;
import com.np3tech.hoopmind.ml.GameOutcomePrediction;
import com.np3tech.hoopmind.ml.GameOutcomePredictor;
import com.np3tech.hoopmind.ml.PredictionFactor;
import com.np3tech.hoopmind.nba.GameHistory;
import com.np3tech.hoopmind.nba.GameRecord;
import com.np3tech.hoopmind.shared.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
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
    public GameOutcomeAnalysis analyze(long seasonId, long homeTeamId, long awayTeamId, LocalDate date, boolean playoff) {
        ScoringCard card = cards.current().orElseThrow(() -> new AppException(
                "Le modèle n'est pas encore entraîné (POST /api/v1/spark/ml/train/game-outcome/jobs).",
                HttpStatus.CONFLICT, "MODEL_NOT_TRAINED"));
        int seasonYear = history.seasonYear(seasonId).orElseThrow(() -> new AppException(
                "Saison introuvable : " + seasonId, HttpStatus.NOT_FOUND, "SEASON_NOT_FOUND"));
        List<Long> teams = List.of(homeTeamId, awayTeamId);
        List<GameRecord> games = history.teamGames(seasonYear, teams);
        List<GameRecord> previousSeason = history.teamGames(seasonYear - 1, teams);

        TeamPrior homePrior = TeamPrior.of(previousSeason, homeTeamId);
        TeamPrior awayPrior = TeamPrior.of(previousSeason, awayTeamId);
        TeamForm home = TeamForm.before(games, homeTeamId, date, null, homePrior);
        TeamForm away = TeamForm.before(games, awayTeamId, date, null, awayPrior);
        // En début de saison, le bilan de la saison passée suffit ; sans aucun des deux, il n'y a rien à comparer.
        if (home.gamesPlayed() == 0 && homePrior.gamesPlayed() == 0
                || away.gamesPlayed() == 0 && awayPrior.gamesPlayed() == 0) {
            throw new AppException("Aucun match joué par ces équipes avant cette date, ni cette saison ni la précédente.",
                    HttpStatus.UNPROCESSABLE_CONTENT, "NO_TEAM_FORM");
        }
        double[] features = GameOutcomeFeatures.of(home, away, playoff);
        double probability = card.homeWinProbability(features);
        GameOutcomePrediction prediction =
                new GameOutcomePrediction(probability >= 0.5, probability, 1 - probability, card.model());
        return new GameOutcomeAnalysis(prediction, card.version(), card.intercept(),
                factors(home, away, playoff, card.contributions(features)), home.gamesPlayed(), away.gamesPlayed());
    }

    /** Valeurs de chaque côté, dans l'ordre de {@link GameOutcomeFeatures#NAMES}. */
    private static List<PredictionFactor> factors(TeamForm home, TeamForm away, boolean playoff, double[] contributions) {
        double[][] sides = {
                {home.winPct(), away.winPct()},
                {home.last10WinPct(), away.last10WinPct()},
                {home.netRating(), away.netRating()},
                {home.restDays(), away.restDays()},
                {playoff ? 1 : 0, playoff ? 1 : 0}
        };
        List<PredictionFactor> factors = new ArrayList<>();
        for (int i = 0; i < GameOutcomeFeatures.NAMES.size(); i++) {
            factors.add(new PredictionFactor(GameOutcomeFeatures.NAMES.get(i), sides[i][0], sides[i][1], contributions[i]));
        }
        return factors;
    }
}
