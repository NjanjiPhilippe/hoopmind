package com.np3tech.hoopmind.nba.mapper;

import com.np3tech.hoopmind.nba.dto.request.TeamCreateRequest;
import com.np3tech.hoopmind.nba.dto.request.TeamUpdateRequest;
import com.np3tech.hoopmind.nba.dto.response.TeamResponse;
import com.np3tech.hoopmind.nba.entity.Team;
import org.springframework.stereotype.Component;

@Component
public class TeamMapper {

    public Team toEntity(TeamCreateRequest request) {
        return Team.builder()
                .name(request.getName())
                .city(request.getCity())
                .abbreviation(request.getAbbreviation())
                .conference(request.getConference())
                .division(request.getDivision())
                .logoUrl(request.getLogoUrl())
                .foundedYear(request.getFoundedYear())
                .arena(request.getArena())
                .headCoach(request.getHeadCoach())
                .build();
    }

    public Team toEntity(TeamUpdateRequest request) {
        return Team.builder()
                .name(request.getName())
                .city(request.getCity())
                .abbreviation(request.getAbbreviation())
                .conference(request.getConference())
                .division(request.getDivision())
                .logoUrl(request.getLogoUrl())
                .arena(request.getArena())
                .headCoach(request.getHeadCoach())
                .build();
    }

    public TeamResponse toResponse(Team team) {
        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .city(team.getCity())
                .abbreviation(team.getAbbreviation())
                .conference(team.getConference())
                .division(team.getDivision())
                .logoUrl(team.getLogoUrl())
                .foundedYear(team.getFoundedYear())
                .arena(team.getArena())
                .headCoach(team.getHeadCoach())
                .build();
    }
}