package com.np3tech.hoopmind.ml.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchPredictionCreateRequest {
    @NotNull(message = "Game ID is required")
    private Long gameId;

    @NotNull(message = "Predicted winner ID is required")
    private Long predictedWinnerId;

    @NotNull(message = "Win probability is required")
    private BigDecimal winProbability;

    private Integer predictedHomeScore;
    private Integer predictedAwayScore;
    private Integer predictedScoreDifference;

    @NotBlank(message = "Model name is required")
    private String model;

    private String modelVersion;
    private String predictionFactorsJson;
    private BigDecimal confidence;
}
