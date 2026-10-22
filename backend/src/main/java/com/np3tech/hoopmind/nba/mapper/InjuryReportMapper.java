package com.np3tech.hoopmind.nba.mapper;

import com.np3tech.hoopmind.nba.dto.response.InjuryReportResponse;
import com.np3tech.hoopmind.nba.dto.response.PlayerSummaryResponse;
import com.np3tech.hoopmind.nba.dto.response.TeamSummaryResponse;
import com.np3tech.hoopmind.nba.entity.InjuryReport;
import com.np3tech.hoopmind.nba.entity.Player;
import com.np3tech.hoopmind.nba.entity.Team;
import org.springframework.stereotype.Component;

@Component
public class InjuryReportMapper {

    public InjuryReportResponse toResponse(InjuryReport injuryReport) {
        return InjuryReportResponse.builder()
                .id(injuryReport.getId())
                .player(toPlayerSummary(injuryReport.getPlayer()))
                .team(toTeamSummary(injuryReport.getTeam()))
                .injuryType(injuryReport.getInjuryType())
                .description(injuryReport.getDescription())
                .status(injuryReport.getStatus())
                .reportDate(injuryReport.getReportDate())
                .expectedReturnDate(injuryReport.getExpectedReturnDate())
                .actualReturnDate(injuryReport.getActualReturnDate())
                .gamesExpectedToMiss(injuryReport.getGamesExpectedToMiss())
                .gamesActuallyMissed(injuryReport.getGamesActuallyMissed())
                .isActive(injuryReport.getIsActive())
                .build();
    }

    private PlayerSummaryResponse toPlayerSummary(Player player) {
        return PlayerSummaryResponse.builder()
                .id(player.getId())
                .firstName(player.getFirstName())
                .lastName(player.getLastName())
                .position(player.getPosition())
                .jerseyNumber(player.getJerseyNumber())
                .photoUrl(player.getPhotoUrl())
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
}