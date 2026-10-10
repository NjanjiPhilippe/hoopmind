package com.np3tech.hoopmind.nba.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerStatsResponse {
    private Long id;
    private PlayerSummaryResponse player;
    private SeasonSummaryResponse season;
    private Integer gamesPlayed;
    private Integer gamesStarted;
    private BigDecimal minutesPerGame;
    private BigDecimal pointsPerGame;
    private BigDecimal totalReboundsPerGame;
    private BigDecimal assistsPerGame;
    private BigDecimal stealsPerGame;
    private BigDecimal blocksPerGame;
    private BigDecimal turnoversPerGame;
    private BigDecimal fieldGoalPercentage;
    private BigDecimal threePointPercentage;
    private BigDecimal freeThrowPercentage;
    private BigDecimal trueShootingPercentage;
    private BigDecimal effectiveFieldGoalPercentage;
}
