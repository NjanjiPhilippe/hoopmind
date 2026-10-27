package com.np3tech.hoopmind.nba.dto.response;

import com.np3tech.hoopmind.nba.entity.Team;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamSummaryResponse {
    private Long id;
    private String name;
    private String city;
    private String abbreviation;
    private String logoUrl;

    public static TeamSummaryResponse of(Team team) {
        return new TeamSummaryResponse(team.getId(), team.getName(), team.getCity(), team.getAbbreviation(), team.getLogoUrl());
    }
}
