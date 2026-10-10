package com.np3tech.hoopmind.ml.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchPredictionDTO {
    private Long id;
    private Long matchId;
    private String homeTeamName;
    private String awayTeamName;
    private LocalDateTime matchDate;
    private Double predictedHomeScore;
    private Double predictedAwayScore;
    private Double homeWinProbability;
    private Double awayWinProbability;
    private Double confidenceScore;
    private LocalDateTime predictionDate;
    private String modelVersion;
    private String factors; // JSON des facteurs
}
