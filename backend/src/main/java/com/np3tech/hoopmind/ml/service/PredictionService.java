package com.np3tech.hoopmind.ml.service;

import com.np3tech.hoopmind.ml.dto.MatchPredictionDTO;

import java.util.List;

public interface PredictionService {
    MatchPredictionDTO predictMatch(Long matchId);
    List<MatchPredictionDTO> getUpcomingPredictions(int limit);
    MatchPredictionDTO getPredictionById(Long predictionId);
}
