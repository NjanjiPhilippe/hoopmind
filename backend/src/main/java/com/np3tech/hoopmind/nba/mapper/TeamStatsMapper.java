package com.np3tech.hoopmind.nba.mapper;

import com.np3tech.hoopmind.nba.dto.response.SeasonSummaryResponse;
import com.np3tech.hoopmind.nba.dto.response.TeamStatsResponse;
import com.np3tech.hoopmind.nba.dto.response.TeamSummaryResponse;
import com.np3tech.hoopmind.nba.entity.Season;
import com.np3tech.hoopmind.nba.entity.Team;
import com.np3tech.hoopmind.nba.entity.TeamStats;
import org.springframework.stereotype.Component;

@Component
public class TeamStatsMapper {

    public TeamStatsResponse toResponse(TeamStats stats) {
        return TeamStatsResponse.builder()
                .id(stats.getId())
                .team(toTeamSummary(stats.getTeam()))
                .season(toSeasonSummary(stats.getSeason()))
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

    private TeamSummaryResponse toTeamSummary(Team team) {
        return TeamSummaryResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .city(team.getCity())
                .abbreviation(team.getAbbreviation())
                .logoUrl(team.getLogoUrl())
                .build();
    }

    private SeasonSummaryResponse toSeasonSummary(Season season) {
        return SeasonSummaryResponse.builder()
                .id(season.getId())
                .year(season.getYear())
                .name(season.getName())
                .build();
    }
}
