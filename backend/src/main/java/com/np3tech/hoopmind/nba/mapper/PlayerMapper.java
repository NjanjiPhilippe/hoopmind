package com.np3tech.hoopmind.nba.mapper;

import com.np3tech.hoopmind.nba.dto.request.PlayerCreateRequest;
import com.np3tech.hoopmind.nba.dto.request.PlayerUpdateRequest;
import com.np3tech.hoopmind.nba.dto.response.PlayerResponse;
import com.np3tech.hoopmind.nba.dto.response.TeamSummaryResponse;
import com.np3tech.hoopmind.nba.entity.Player;
import com.np3tech.hoopmind.nba.entity.Team;
import com.np3tech.hoopmind.nba.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlayerMapper {

    private final TeamService teamService;

    public Player toEntity(PlayerCreateRequest request) {
        Team team = teamService.getTeamById(request.getTeamId());

        return Player.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .position(request.getPosition())
                .jerseyNumber(request.getJerseyNumber())
                .team(team)
                .birthDate(request.getBirthDate())
                .birthPlace(request.getBirthPlace())
                .nationality(request.getNationality())
                .height(request.getHeight())
                .weight(request.getWeight())
                .experience(request.getExperience())
                .college(request.getCollege())
                .draftYear(request.getDraftYear())
                .draftRound(request.getDraftRound())
                .draftNumber(request.getDraftNumber())
                .photoUrl(request.getPhotoUrl())
                .build();
    }

    public Player toEntity(PlayerUpdateRequest request) {
        Player.PlayerBuilder builder = Player.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .position(request.getPosition())
                .jerseyNumber(request.getJerseyNumber())
                .height(request.getHeight())
                .weight(request.getWeight())
                .photoUrl(request.getPhotoUrl());

        if (request.getTeamId() != null) {
            Team team = teamService.getTeamById(request.getTeamId());
            builder.team(team);
        }

        return builder.build();
    }

    public PlayerResponse toResponse(Player player) {
        return PlayerResponse.builder()
                .id(player.getId())
                .firstName(player.getFirstName())
                .lastName(player.getLastName())
                .position(player.getPosition())
                .jerseyNumber(player.getJerseyNumber())
                .team(toTeamSummary(player.getTeam()))
                .birthDate(player.getBirthDate())
                .birthPlace(player.getBirthPlace())
                .nationality(player.getNationality())
                .height(player.getHeight())
                .weight(player.getWeight())
                .experience(player.getExperience())
                .college(player.getCollege())
                .draftYear(player.getDraftYear())
                .draftRound(player.getDraftRound())
                .draftNumber(player.getDraftNumber())
                .photoUrl(player.getPhotoUrl())
                .status(player.getStatus())
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
