package com.np3tech.hoopmind.ml.spark;

import com.np3tech.hoopmind.ml.GameOutcomePrediction;
import com.np3tech.hoopmind.ml.GameOutcomePredictor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Adaptateur Spark ML du port {@link GameOutcomePredictor} (forêt aléatoire entraînée par saison).
 */
@Component
@ConditionalOnSpark
@RequiredArgsConstructor
class SparkGameOutcomePredictor implements GameOutcomePredictor {

    private final SparkMLPredictionService mlService;

    @Override
    public GameOutcomePrediction predict(Long seasonId, Long homeTeamId, Long awayTeamId) {
        Map<String, Object> result = mlService.predictGameOutcome(seasonId, homeTeamId, awayTeamId);
        return new GameOutcomePrediction(
                (Boolean) result.get("homeTeamWins"),
                ((Number) result.get("homeWinProbability")).doubleValue(),
                ((Number) result.get("awayWinProbability")).doubleValue(),
                "spark-random-forest-season-" + seasonId);
    }
}
