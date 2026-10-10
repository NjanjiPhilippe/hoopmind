package com.np3tech.hoopmind.nba.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerFormDTO {
    private Long playerId;
    private String playerName;
    private String season;
    private Integer gamesPlayed;
    private Double pointsPerGame;
    private Double reboundsPerGame;
    private Double assistsPerGame;
    private Double per; // Player Efficiency Rating
    private Double efficiency;
    private String form; // EXCELLENT, TRÈS BON, BON, MOYEN, FAIBLE
    private Double fieldGoalPercentage;
    private Double threePointPercentage;
    private Double freeThrowPercentage;
}