package com.np3tech.hoopmind.ml.service;

import com.np3tech.hoopmind.ml.entity.MatchPrediction;

import java.util.List;

public interface MatchPredictionService {
    MatchPrediction createPrediction(MatchPrediction prediction);
    MatchPrediction updatePrediction(Long id, MatchPrediction predictionUpdate);
    MatchPrediction getPredictionById(Long id);
    MatchPrediction getPredictionByGame(Long gameId);
    List<MatchPrediction> getPredictionsByModel(String model);
    List<MatchPrediction> getEvaluatedPredictions();
    void evaluatePrediction(Long predictionId);
    void deletePrediction(Long id);
}
