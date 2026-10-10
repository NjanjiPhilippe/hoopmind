package com.np3tech.hoopmind.nba.service;

import com.np3tech.hoopmind.nba.entity.TeamStats;
import com.np3tech.hoopmind.nba.entity.enummeration.Conference;

import java.util.List;

public interface TeamStatsService {
    TeamStats createOrUpdateTeamStats(TeamStats teamStats);
    TeamStats getTeamStatsById(Long id);
    TeamStats getTeamStatsByTeamAndSeason(Long teamId, Long seasonId);
    List<TeamStats> getTeamStatsBySeason(Long seasonId);
    List<TeamStats> getStandingsBySeason(Long seasonId);
    List<TeamStats> getConferenceStandings(Long seasonId, Conference conference);
    void deleteTeamStats(Long id);
}
