package com.np3tech.hoopmind.nba.service;

import com.np3tech.hoopmind.nba.dto.HeadToHeadDTO;
import com.np3tech.hoopmind.nba.dto.MatchDTO;

import java.util.List;

public interface MatchService {
    List<MatchDTO> getUpcomingMatches(int limit);
    List<MatchDTO> getMatchesBySeason(String season);
    MatchDTO getMatchById(Long id);
    List<MatchDTO> getTeamMatches(Long teamId, String season);
    HeadToHeadDTO getHeadToHeadAnalysis(Long team1Id, Long team2Id, int limit);
    MatchDTO createMatch(MatchDTO matchDTO);
    MatchDTO updateMatchScore(Long matchId, Integer homeScore, Integer awayScore);
}
