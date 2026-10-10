package com.np3tech.hoopmind.nba.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Reports", description = "Reporting and analytics APIs")
@RequestMapping("/api/v1/reports")
public interface ReportApi {

    @Operation(summary = "Get season summary")
    @GetMapping("/season/{seasonId}/summary")
    ResponseEntity<Map<String, Object>> getSeasonSummary(@PathVariable Long seasonId);

    @Operation(summary = "Get player leaderboard")
    @GetMapping("/season/{seasonId}/leaderboard")
    ResponseEntity<Map<String, Object>> getPlayerLeaderboard(
            @PathVariable Long seasonId,
            @RequestParam(defaultValue = "10") int limit
    );

    @Operation(summary = "Get injury report")
    @GetMapping("/injuries")
    ResponseEntity<Map<String, Object>> getInjuryReport();

    @Operation(summary = "Get head-to-head statistics")
    @GetMapping("/head-to-head")
    ResponseEntity<Map<String, Object>> getHeadToHeadStats(
            @RequestParam Long team1Id,
            @RequestParam Long team2Id,
            @RequestParam Long seasonId
    );
}
