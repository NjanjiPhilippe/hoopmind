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
public class TeamStatsResponse {
    private Long id;
    private TeamSummaryResponse team;
    private SeasonSummaryResponse season;
    private Integer wins;
    private Integer losses;
    private BigDecimal winPercentage;
    private Integer conferenceRank;
    private Integer divisionRank;
    private Integer homeWins;
    private Integer homeLosses;
    private Integer awayWins;
    private Integer awayLosses;
    private BigDecimal pointsPerGame;
    private BigDecimal pointsAllowedPerGame;
    private BigDecimal pointsDifferential;
    private BigDecimal offensiveRating;
    private BigDecimal defensiveRating;
    private BigDecimal netRating;
    private Integer currentStreak;
}
