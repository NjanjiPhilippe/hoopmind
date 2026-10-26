package com.np3tech.hoopmind.nba.controller;

import com.np3tech.hoopmind.nba.dto.response.TeamStatsResponse;
import com.np3tech.hoopmind.nba.mapper.TeamStatsMapper;
import com.np3tech.hoopmind.nba.service.TeamStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
@Tag(name = "Statistics", description = "Statistics APIs")
class StatsController {

    private final TeamStatsService teamStatsService;
    private final TeamStatsMapper teamStatsMapper;

    @GetMapping("/standings/season/{seasonId}")
    @Operation(operationId = "getStandingsBySeason", summary = "Get standings for season")
    List<TeamStatsResponse> getStandingsBySeason(@PathVariable Long seasonId) {
        return teamStatsService.getStandingsBySeason(seasonId).stream().map(teamStatsMapper::toResponse).toList();
    }
}
