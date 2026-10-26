package com.np3tech.hoopmind.nba.mapper;

import com.np3tech.hoopmind.nba.dto.response.SeasonSummaryResponse;
import com.np3tech.hoopmind.nba.dto.response.TeamStatsResponse;
import com.np3tech.hoopmind.nba.dto.response.TeamSummaryResponse;
import com.np3tech.hoopmind.nba.entity.TeamStats;
import org.springframework.stereotype.Component;

@Component
public class TeamStatsMapper {

    public TeamStatsResponse toResponse(TeamStats stats) {
        return TeamStatsResponse.builder()
                .id(stats.getId())
                .team(TeamSummaryResponse.of(stats.getTeam()))
                .season(SeasonSummaryResponse.of(stats.getSeason()))
                .wins(stats.getWins())
                .losses(stats.getLosses())
                .winPercentage(stats.getWinPercentage())
                .conferenceRank(stats.getConferenceRank())
                .divisionRank(stats.getDivisionRank())
                .homeWins(stats.getHomeWins())
                .homeLosses(stats.getHomeLosses())
                .awayWins(stats.getAwayWins())
                .awayLosses(stats.getAwayLosses())
                .pointsPerGame(stats.getPointsPerGame())
                .pointsAllowedPerGame(stats.getPointsAllowedPerGame())
                .pointsDifferential(stats.getPointsDifferential())
                .offensiveRating(stats.getOffensiveRating())
                .defensiveRating(stats.getDefensiveRating())
                .netRating(stats.getNetRating())
                .currentStreak(stats.getCurrentStreak())
                .build();
    }
}
