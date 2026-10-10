package com.np3tech.hoopmind.ml.dto.response;

import com.np3tech.hoopmind.nba.dto.response.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchPredictionResponse {
    private Long id;
    private GameSummaryResponse game;
    private TeamSummaryResponse predictedWinner;
    private BigDecimal winProbability;
    private Integer predictedHomeScore;
    private Integer predictedAwayScore;
    private Integer predictedScoreDifference;
    private String model;
    private String modelVersion;
    private BigDecimal confidence;
    private LocalDateTime predictionDate;
    private Boolean isAccurate;
    private Boolean predictedWinnerCorrect;
    private Integer actualScoreDifference;
    private BigDecimal predictionError;
}
