package com.np3tech.hoopmind.ml.service;

import com.np3tech.hoopmind.ml.dto.PredictionFactorsDTO;
import com.np3tech.hoopmind.nba.entity.Game;

import java.util.Map;

public interface SparkAnalyticsService {
    void trainPredictionModel(String season);

    Map<String, Object> refinePrediction(Game game, double homeScore, double awayScore,
                                         PredictionFactorsDTO factors);

    Map<String, Object> analyzeTeamPatterns(Long teamId, String season);

    Map<String, Double> calculateAdvancedMetrics(Long teamId, String season);
}
