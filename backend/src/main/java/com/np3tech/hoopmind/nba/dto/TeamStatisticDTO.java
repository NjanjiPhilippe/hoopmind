package com.np3tech.hoopmind.nba.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamStatisticDTO {
    private Long id;
    private Long teamId;
    private String teamName;
    private String season;
    private Integer wins;
    private Integer losses;
    private BigDecimal winPercentage;
    private BigDecimal pointsPerGame;
    private BigDecimal pointsAllowedPerGame;
    private BigDecimal fieldGoalPercentage;
    private BigDecimal threePointPercentage;
    private BigDecimal freeThrowPercentage;
    private BigDecimal reboundsPerGame;
    private BigDecimal assistsPerGame;
    private String homeRecord;
    private String awayRecord;
}