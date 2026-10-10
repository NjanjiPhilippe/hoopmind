package com.np3tech.hoopmind.nba.controller;

import com.np3tech.hoopmind.nba.api.ReportApi;
import com.np3tech.hoopmind.nba.entity.PlayerStats;
import com.np3tech.hoopmind.nba.entity.Season;
import com.np3tech.hoopmind.nba.entity.Team;
import com.np3tech.hoopmind.nba.service.ReportingService;
import com.np3tech.hoopmind.nba.service.SeasonService;
import com.np3tech.hoopmind.nba.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ReportController implements ReportApi {

    private final ReportingService reportingService;
    private final SeasonService seasonService;
    private final TeamService teamService;

    @Override
    public ResponseEntity<Map<String, Object>> getSeasonSummary(Long seasonId) {
        log.debug("Generating season summary for season: {}", seasonId);

        Season season = seasonService.getSeasonById(seasonId);
        Map<String, Object> summary = reportingService.generateSeasonSummary(season);

        return ResponseEntity.ok(summary);
    }

    @Override
    public ResponseEntity<Map<String, Object>> getPlayerLeaderboard(Long seasonId, int limit) {
        log.debug("Generating player leaderboard for season: {} with limit: {}", seasonId, limit);

        Season season = seasonService.getSeasonById(seasonId);
        Map<String, List<PlayerStats>> leaderboard = reportingService.generatePlayerLeaderboard(season, limit);

        return ResponseEntity.ok(Map.of("leaderboard", leaderboard));
    }

    @Override
    public ResponseEntity<Map<String, Object>> getInjuryReport() {
        log.debug("Generating injury report");

        Map<String, Object> report = reportingService.generateInjuryReport(null);
        return ResponseEntity.ok(report);
    }

    @Override
    public ResponseEntity<Map<String, Object>> getHeadToHeadStats(Long team1Id, Long team2Id, Long seasonId) {
        log.debug("Generating head-to-head stats: {} vs {} in season {}", team1Id, team2Id, seasonId);

        Team team1 = teamService.getTeamById(team1Id);
        Team team2 = teamService.getTeamById(team2Id);
        Season season = seasonService.getSeasonById(seasonId);

        Map<String, Object> stats = reportingService.generateHeadToHeadStats(team1, team2, season);
        return ResponseEntity.ok(stats);
    }
}