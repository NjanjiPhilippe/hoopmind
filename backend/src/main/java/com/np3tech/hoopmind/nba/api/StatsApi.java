package com.np3tech.hoopmind.nba.api;

import com.np3tech.hoopmind.nba.dto.response.PlayerStatsResponse;
import com.np3tech.hoopmind.nba.dto.response.TeamStatsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Statistics", description = "Statistics APIs")
@RequestMapping("/api/v1/stats")
public interface StatsApi {

    @Operation(summary = "Get team stats by season")
    @GetMapping("/teams/season/{seasonId}")
    ResponseEntity<List<TeamStatsResponse>> getTeamStatsBySeason(@PathVariable Long seasonId);

    @Operation(summary = "Get team stats for specific team and season")
    @GetMapping("/teams/{teamId}/season/{seasonId}")
    ResponseEntity<TeamStatsResponse> getTeamStats(
            @PathVariable Long teamId,
            @PathVariable Long seasonId
    );

    @Operation(summary = "Get standings for season")
    @GetMapping("/standings/season/{seasonId}")
    ResponseEntity<List<TeamStatsResponse>> getStandingsBySeason(@PathVariable Long seasonId);

    @Operation(summary = "Get player stats by season")
    @GetMapping("/players/season/{seasonId}")
    ResponseEntity<List<PlayerStatsResponse>> getPlayerStatsBySeason(@PathVariable Long seasonId);

    @Operation(summary = "Get player stats for specific player and season")
    @GetMapping("/players/{playerId}/season/{seasonId}")
    ResponseEntity<PlayerStatsResponse> getPlayerStats(
            @PathVariable Long playerId,
            @PathVariable Long seasonId
    );

    @Operation(summary = "Get top scorers")
    @GetMapping("/leaders/scorers/season/{seasonId}")
    ResponseEntity<List<PlayerStatsResponse>> getTopScorers(
            @PathVariable Long seasonId,
            @RequestParam(defaultValue = "10") int limit
    );

    @Operation(summary = "Get top rebounders")
    @GetMapping("/leaders/rebounders/season/{seasonId}")
    ResponseEntity<List<PlayerStatsResponse>> getTopRebounders(
            @PathVariable Long seasonId,
            @RequestParam(defaultValue = "10") int limit
    );

    @Operation(summary = "Get top assists")
    @GetMapping("/leaders/assists/season/{seasonId}")
    ResponseEntity<List<PlayerStatsResponse>> getTopAssists(
            @PathVariable Long seasonId,
            @RequestParam(defaultValue = "10") int limit
    );
}