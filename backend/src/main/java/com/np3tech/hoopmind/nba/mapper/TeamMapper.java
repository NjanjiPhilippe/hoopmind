package com.np3tech.hoopmind.nba.mapper;

import com.np3tech.hoopmind.nba.dto.response.TeamResponse;
import com.np3tech.hoopmind.nba.entity.Team;
import org.springframework.stereotype.Component;

@Component
public class TeamMapper {

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