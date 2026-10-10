package com.np3tech.hoopmind.nba.service;

import com.np3tech.hoopmind.nba.dto.*;

import java.util.List;

public interface StatisticsService {
    StatisticsOverviewDTO getTeamStatisticsOverview(Long teamId, Integer season);
    List<TeamStatisticDTO> getTopTeams(Integer season, int limit);
    List<PlayerStatisticDTO> getTopScorers(Integer season, int limit);
    List<PlayerStatisticDTO> getTopAssistants(Integer season, int limit);
    List<TeamStatisticDTO> getConferenceStandings(String conference, Integer season);

}
