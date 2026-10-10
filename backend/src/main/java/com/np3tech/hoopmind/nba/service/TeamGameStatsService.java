package com.np3tech.hoopmind.nba.service;

import com.np3tech.hoopmind.nba.entity.TeamGameStats;

import java.util.List;

public interface TeamGameStatsService {
    TeamGameStats createTeamGameStats(TeamGameStats stats);
    TeamGameStats updateTeamGameStats(Long id, TeamGameStats statsUpdate);
    TeamGameStats getTeamGameStatsById(Long id);
    List<TeamGameStats> getTeamGameStatsByGame(Long gameId);
    List<TeamGameStats> getTeamGameStatsByTeam(Long teamId);
    void deleteTeamGameStats(Long id);
}
