package com.np3tech.hoopmind.nba.controller;

import com.np3tech.hoopmind.nba.dto.request.TeamCreateRequest;
import com.np3tech.hoopmind.nba.dto.request.TeamUpdateRequest;
import com.np3tech.hoopmind.nba.dto.response.TeamResponse;
import com.np3tech.hoopmind.nba.mapper.TeamMapper;
import com.np3tech.hoopmind.nba.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
@Tag(name = "Teams", description = "Team management APIs")
class TeamController {

    private final TeamService teamService;
    private final TeamMapper teamMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(operationId = "createTeam", summary = "Create a new team")
    TeamResponse createTeam(@Valid @RequestBody TeamCreateRequest request) {
        return teamMapper.toResponse(teamService.createTeam(request));
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getTeamById", summary = "Get team by ID")
    TeamResponse getTeamById(@PathVariable Long id) {
        return teamMapper.toResponse(teamService.getTeamById(id));
    }

    @GetMapping
    @Operation(operationId = "getAllTeams", summary = "Get all teams")
    List<TeamResponse> getAllTeams() {
        return teamService.getAllTeams().stream().map(teamMapper::toResponse).toList();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(operationId = "updateTeam", summary = "Update team")
    TeamResponse updateTeam(@PathVariable Long id, @Valid @RequestBody TeamUpdateRequest request) {
        return teamMapper.toResponse(teamService.updateTeam(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(operationId = "deleteTeam", summary = "Delete team")
    void deleteTeam(@PathVariable Long id) {
        teamService.deleteTeam(id);
    }
}
