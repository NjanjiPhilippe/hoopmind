package com.np3tech.hoopmind.nba.dto;

import com.np3tech.hoopmind.ml.dto.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {
    private int totalTeams;
    private int totalPlayers;
    private int upcomingMatches;
    private int totalPredictions;
    private List<MatchPredictionDTO> featuredPredictions;
    private List<TeamDTO> topTeams;
    private List<PlayerDTO> hotPlayers;
}