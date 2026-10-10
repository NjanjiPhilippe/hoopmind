package com.np3tech.hoopmind.nba.controller;

import com.np3tech.hoopmind.nba.api.StatsApi;
import com.np3tech.hoopmind.nba.mapper.PlayerStatsMapper;
import com.np3tech.hoopmind.nba.mapper.TeamStatsMapper;
import com.np3tech.hoopmind.nba.dto.response.PlayerStatsResponse;
import com.np3tech.hoopmind.nba.dto.response.TeamStatsResponse;
import com.np3tech.hoopmind.nba.entity.PlayerStats;
import com.np3tech.hoopmind.nba.entity.TeamStats;
import com.np3tech.hoopmind.nba.service.PlayerStatsService;
import com.np3tech.hoopmind.nba.service.TeamStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class StatsController implements StatsApi {

    private final TeamStatsService teamStatsService;
    private final PlayerStatsService playerStatsService;
    private final TeamStatsMapper teamStatsMapper;
    private final PlayerStatsMapper playerStatsMapper;

    @Override
    public ResponseEntity<List<TeamStatsResponse>> getTeamStatsBySeason(Long seasonId) {
        log.debug("Fetching team stats for season: {}", seasonId);

        List<TeamStats> stats = teamStatsService.getTeamStatsBySeason(seasonId);
        List<TeamStatsResponse> responses = stats.stream()
                .map(teamStatsMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<TeamStatsResponse> getTeamStats(Long teamId, Long seasonId) {
        log.debug("Fetching team stats for team {} in season {}", teamId, seasonId);

        TeamStats stats = teamStatsService.getTeamStatsByTeamAndSeason(teamId, seasonId);
        return ResponseEntity.ok(teamStatsMapper.toResponse(stats));
    }

    @Override
    public ResponseEntity<List<TeamStatsResponse>> getStandingsBySeason(Long seasonId) {
        log.debug("Fetching standings for season: {}", seasonId);

        List<TeamStats> standings = teamStatsService.getStandingsBySeason(seasonId);
        List<TeamStatsResponse> responses = standings.stream()
                .map(teamStatsMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<List<PlayerStatsResponse>> getPlayerStatsBySeason(Long seasonId) {
        log.debug("Fetching player stats for season: {}", seasonId);

        List<PlayerStats> stats = playerStatsService.getPlayerStatsBySeason(seasonId);
        List<PlayerStatsResponse> responses = stats.stream()
                .map(playerStatsMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<PlayerStatsResponse> getPlayerStats(Long playerId, Long seasonId) {
        log.debug("Fetching player stats for player {} in season {}", playerId, seasonId);

        PlayerStats stats = playerStatsService.getPlayerStatsByPlayerAndSeason(playerId, seasonId);
        return ResponseEntity.ok(playerStatsMapper.toResponse(stats));
    }

    @Override
    public ResponseEntity<List<PlayerStatsResponse>> getTopScorers(Long seasonId, int limit) {
        log.debug("Fetching top {} scorers for season {}", limit, seasonId);

        List<PlayerStats> scorers = playerStatsService.getTopScorers(seasonId);
        List<PlayerStatsResponse> responses = scorers.stream()
                .limit(limit)
                .map(playerStatsMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<List<PlayerStatsResponse>> getTopRebounders(Long seasonId, int limit) {
        log.debug("Fetching top {} rebounders for season {}", limit, seasonId);

        List<PlayerStats> rebounders = playerStatsService.getTopRebounders(seasonId);
        List<PlayerStatsResponse> responses = rebounders.stream()
                .limit(limit)
                .map(playerStatsMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<List<PlayerStatsResponse>> getTopAssists(Long seasonId, int limit) {
        log.debug("Fetching top {} assists for season {}", limit, seasonId);

        List<PlayerStats> assists = playerStatsService.getTopAssists(seasonId);
        List<PlayerStatsResponse> responses = assists.stream()
                .limit(limit)
                .map(playerStatsMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }
}