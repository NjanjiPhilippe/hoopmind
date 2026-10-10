package com.np3tech.hoopmind.nba.mapper;

import com.np3tech.hoopmind.nba.dto.request.InjuryReportCreateRequest;
import com.np3tech.hoopmind.nba.dto.request.InjuryReportUpdateRequest;
import com.np3tech.hoopmind.nba.dto.response.InjuryReportResponse;
import com.np3tech.hoopmind.nba.dto.response.PlayerSummaryResponse;
import com.np3tech.hoopmind.nba.dto.response.TeamSummaryResponse;
import com.np3tech.hoopmind.nba.entity.InjuryReport;
import com.np3tech.hoopmind.nba.entity.Player;
import com.np3tech.hoopmind.nba.entity.Team;
import com.np3tech.hoopmind.nba.service.PlayerService;
import com.np3tech.hoopmind.nba.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InjuryReportMapper {

    private final PlayerService playerService;
    private final TeamService teamService;

    public InjuryReport toEntity(InjuryReportCreateRequest request) {
        Player player = playerService.getPlayerById(request.getPlayerId());
        Team team = teamService.getTeamById(request.getTeamId());

        return InjuryReport.builder()
                .player(player)
                .team(team)
                .injuryType(request.getInjuryType())
                .description(request.getDescription())
                .status(request.getStatus())
                .reportDate(request.getReportDate())
                .expectedReturnDate(request.getExpectedReturnDate())
                .gamesExpectedToMiss(request.getGamesExpectedToMiss())
                .build();
    }

    public InjuryReport toEntity(InjuryReportUpdateRequest request) {
        return InjuryReport.builder()
                .status(request.getStatus())
                .description(request.getDescription())
                .expectedReturnDate(request.getExpectedReturnDate())
                .actualReturnDate(request.getActualReturnDate())
                .gamesExpectedToMiss(request.getGamesExpectedToMiss())
                .gamesActuallyMissed(request.getGamesActuallyMissed())
                .isActive(request.getIsActive())
                .build();
    }

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