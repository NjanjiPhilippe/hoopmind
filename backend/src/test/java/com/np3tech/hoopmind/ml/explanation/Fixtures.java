package com.np3tech.hoopmind.ml.explanation;

import com.np3tech.hoopmind.ml.GameOutcomeAnalysis;
import com.np3tech.hoopmind.ml.GameOutcomePrediction;
import com.np3tech.hoopmind.ml.PredictionFactor;
import com.np3tech.hoopmind.ml.ScorePrediction;

import java.time.LocalDate;
import java.util.List;

/** Une prédiction type : Boston favori à domicile, surtout grâce à son net rating, malgré moins de repos. */
final class Fixtures {

    static final LocalDate DAY = LocalDate.of(2025, 1, 15);

    private Fixtures() {
    }

    static GameOutcomeAnalysis analysis() {
        return new GameOutcomeAnalysis(new GameOutcomePrediction(true, 0.71, 0.29, "logistic-regression"), "v1", 0.12,
                List.of(new PredictionFactor("win_pct_diff", 0.68, 0.52, 0.31),
                        new PredictionFactor("last10_win_pct_diff", 0.7, 0.6, 0.02),
                        new PredictionFactor("net_rating_diff", 9.4, 1.2, 0.48),
                        new PredictionFactor("rest_days_diff", 1, 3, -0.22),
                        new PredictionFactor("is_playoff", 0, 0, -0.01)), 30, 29);
    }

    static ExplanationRequest request() {
        return new ExplanationRequest("Boston Celtics", "New York Knicks", DAY, false, analysis());
    }

    /** La même prédiction avec un score estimé. */
    static ExplanationRequest requestWithScore(ScorePrediction score) {
        GameOutcomeAnalysis analysis = analysis();
        GameOutcomePrediction prediction = analysis.prediction();
        return new ExplanationRequest("Boston Celtics", "New York Knicks", DAY, false, new GameOutcomeAnalysis(
                new GameOutcomePrediction(prediction.homeTeamWins(), prediction.homeWinProbability(),
                        prediction.awayWinProbability(), prediction.model(), score),
                analysis.modelVersion(), analysis.intercept(), analysis.factors(), analysis.homeGamesPlayed(),
                analysis.awayGamesPlayed()));
    }
}
