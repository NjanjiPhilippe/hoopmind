package com.np3tech.hoopmind.nba.api;

import com.np3tech.hoopmind.nba.dto.request.InjuryReportCreateRequest;
import com.np3tech.hoopmind.nba.dto.request.InjuryReportUpdateRequest;
import com.np3tech.hoopmind.nba.dto.response.InjuryReportResponse;
import com.np3tech.hoopmind.nba.entity.enummeration.InjuryStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "Injury Reports", description = "Injury report management APIs")
@RequestMapping("/api/v1/injuries")
public interface InjuryReportApi {

    @Operation(summary = "Create a new injury report")
    @PostMapping
    ResponseEntity<InjuryReportResponse> createInjuryReport(@Valid @RequestBody InjuryReportCreateRequest request);

    @Operation(summary = "Get injury report by ID")
    @GetMapping("/{id}")
    ResponseEntity<InjuryReportResponse> getInjuryReportById(@PathVariable Long id);

    @Operation(summary = "Update injury report")
    @PutMapping("/{id}")
    ResponseEntity<InjuryReportResponse> updateInjuryReport(@PathVariable Long id, @Valid @RequestBody InjuryReportUpdateRequest request);

    @Operation(summary = "Get injury reports by player")
    @GetMapping("/player/{playerId}")
    ResponseEntity<List<InjuryReportResponse>> getInjuryReportsByPlayer(@PathVariable Long playerId);

    @Operation(summary = "Get active injury reports by player")
    @GetMapping("/player/{playerId}/active")
    ResponseEntity<List<InjuryReportResponse>> getActiveInjuryReportsByPlayer(@PathVariable Long playerId);

    @Operation(summary = "Get injury reports by team")
    @GetMapping("/team/{teamId}")
    ResponseEntity<List<InjuryReportResponse>> getInjuryReportsByTeam(@PathVariable Long teamId);

    @Operation(summary = "Get current team injuries")
    @GetMapping("/team/{teamId}/current")
    ResponseEntity<List<InjuryReportResponse>> getCurrentTeamInjuries(@PathVariable Long teamId);

    @Operation(summary = "Get injury reports by status")
    @GetMapping("/status/{status}")
    ResponseEntity<List<InjuryReportResponse>> getInjuryReportsByStatus(@PathVariable InjuryStatus status);

    @Operation(summary = "Deactivate injury report")
    @PatchMapping("/{id}/deactivate")
    ResponseEntity<Void> deactivateInjuryReport(@PathVariable Long id);

    @Operation(summary = "Delete injury report")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteInjuryReport(@PathVariable Long id);
}
