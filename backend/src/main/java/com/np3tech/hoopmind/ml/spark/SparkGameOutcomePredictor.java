package com.np3tech.hoopmind.ml.spark;

import com.np3tech.hoopmind.ml.GameOutcomePrediction;
import com.np3tech.hoopmind.ml.GameOutcomePredictor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Adaptateur Spark ML du port {@link GameOutcomePredictor} : régression logistique entraînée sur plusieurs saisons,
 * appliquée à la forme actuelle des deux équipes.
 */
@Component
@ConditionalOnSpark
@RequiredArgsConstructor
class SparkGameOutcomePredictor implements GameOutcomePredictor {

    private final GameOutcomeModelService model;

    @Override
    public GameOutcomePrediction predict(Long seasonId, Long homeTeamId, Long awayTeamId) {
        double home = model.homeWinProbability(seasonId, homeTeamId, awayTeamId);
        return new GameOutcomePrediction(home >= 0.5, home, 1 - home, GameOutcomeModelService.MODEL_NAME);
    }
}
