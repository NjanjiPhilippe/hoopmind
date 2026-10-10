package com.np3tech.hoopmind.nba.api;

import com.np3tech.hoopmind.nba.dto.request.TeamCreateRequest;
import com.np3tech.hoopmind.nba.dto.request.TeamUpdateRequest;
import com.np3tech.hoopmind.nba.dto.response.TeamResponse;
import com.np3tech.hoopmind.nba.entity.enummeration.Conference;
import com.np3tech.hoopmind.nba.entity.enummeration.Division;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "Teams", description = "Team management APIs")
@RequestMapping("/api/v1/teams")
public interface TeamApi {

    @Operation(summary = "Create a new team")
    @PostMapping
    ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody TeamCreateRequest request);

    @Operation(summary = "Get team by ID")
    @GetMapping("/{id}")
    ResponseEntity<TeamResponse> getTeamById(@PathVariable Long id);

    @Operation(summary = "Get all teams")
    @GetMapping
    ResponseEntity<List<TeamResponse>> getAllTeams();

    @Operation(summary = "Update team")
    @PutMapping("/{id}")
    ResponseEntity<TeamResponse> updateTeam(@PathVariable Long id, @Valid @RequestBody TeamUpdateRequest request);

    @Operation(summary = "Delete team")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteTeam(@PathVariable Long id);

    @Operation(summary = "Get teams by conference")
    @GetMapping("/conference/{conference}")
    ResponseEntity<List<TeamResponse>> getTeamsByConference(@PathVariable Conference conference);

    @Operation(summary = "Get teams by division")
    @GetMapping("/division/{division}")
    ResponseEntity<List<TeamResponse>> getTeamsByDivision(@PathVariable Division division);

    @Operation(summary = "Get team by abbreviation")
    @GetMapping("/abbreviation/{abbreviation}")
    ResponseEntity<TeamResponse> getTeamByAbbreviation(@PathVariable String abbreviation);
}
