package com.np3tech.hoopmind.nba.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerStatisticDTO {
    private Long id;
    private Long playerId;
    private String playerName;
    private String season;
    private Integer gamesPlayed;
    private BigDecimal minutesPerGame;
    private BigDecimal pointsPerGame;
    private BigDecimal reboundsPerGame;
    private BigDecimal assistsPerGame;
    private BigDecimal stealsPerGame;
    private BigDecimal blocksPerGame;
    private BigDecimal fieldGoalPercentage;
    private BigDecimal threePointPercentage;
    private BigDecimal freeThrowPercentage;
    private BigDecimal turnoversPerGame;
    private LocalDate statDate;
}