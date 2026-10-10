package com.np3tech.hoopmind.nba.controller;

import com.np3tech.hoopmind.shared.dto.ApiResponse;
import com.np3tech.hoopmind.nba.dto.PlayerStatisticDTO;
import com.np3tech.hoopmind.nba.dto.StatisticsOverviewDTO;
import com.np3tech.hoopmind.nba.dto.TeamStatisticDTO;
import com.np3tech.hoopmind.nba.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
@Slf4j
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/team/{teamId}")
    public ResponseEntity<ApiResponse<StatisticsOverviewDTO>> getTeamStatisticsOverview(
            @PathVariable Long teamId,
            @RequestParam Integer season) {
        log.info("Récupération vue d'ensemble statistiques équipe {} - saison {}", teamId, season);
        StatisticsOverviewDTO overview = statisticsService.getTeamStatisticsOverview(teamId, season);
        return ResponseEntity.ok(ApiResponse.success("Statistiques récupérées avec succès", overview));
    }

    @GetMapping("/season/top-teams")
    public ResponseEntity<ApiResponse<List<TeamStatisticDTO>>> getTopTeams(
            @RequestParam(name = "year") Integer year,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Récupération du top {} équipes pour la saison {}", limit, year);
        List<TeamStatisticDTO> topTeams = statisticsService.getTopTeams(year, limit);
        return ResponseEntity.ok(ApiResponse.success("Top équipes récupéré avec succès", topTeams));
    }

    @GetMapping("/season/top-scorers")
    public ResponseEntity<ApiResponse<List<PlayerStatisticDTO>>> getTopScorers(
            @RequestParam(name = "year") Integer year,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Récupération du top {} scoreurs pour la saison {}", limit, year);
        List<PlayerStatisticDTO> topScorers = statisticsService.getTopScorers(year, limit);
        return ResponseEntity.ok(ApiResponse.success("Top scoreurs récupéré avec succès", topScorers));
    }

    @GetMapping("/season/top-assistants")
    public ResponseEntity<ApiResponse<List<PlayerStatisticDTO>>> getTopAssistants(
            @RequestParam(name = "year") Integer year,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Récupération du top {} passeurs pour la saison {}", limit, year);
        List<PlayerStatisticDTO> topAssistants = statisticsService.getTopAssistants(year, limit);
        return ResponseEntity.ok(ApiResponse.success("Top passeurs récupéré avec succès", topAssistants));
    }

    @GetMapping("/conference")
    public ResponseEntity<ApiResponse<List<TeamStatisticDTO>>> getConferenceStandings(
            @RequestParam String conference,
            @RequestParam Integer year) {
        log.info("Récupération du classement de la conférence {} - saison {}", conference, year);
        List<TeamStatisticDTO> standings = statisticsService.getConferenceStandings(conference, year);
        return ResponseEntity.ok(ApiResponse.success("Classement récupéré avec succès", standings));
    }
}
