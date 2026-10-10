package com.np3tech.hoopmind.nba.api;

import com.np3tech.hoopmind.nba.dto.request.SeasonCreateRequest;
import com.np3tech.hoopmind.nba.dto.request.SeasonUpdateRequest;
import com.np3tech.hoopmind.nba.dto.response.SeasonResponse;
import com.np3tech.hoopmind.nba.entity.enummeration.SeasonStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "Seasons", description = "Season management APIs")
@RequestMapping("/api/v1/seasons")
public interface SeasonApi {

    @Operation(summary = "Create a new season")
    @PostMapping
    ResponseEntity<SeasonResponse> createSeason(@Valid @RequestBody SeasonCreateRequest request);

    @Operation(summary = "Get season by ID")
    @GetMapping("/{id}")
    ResponseEntity<SeasonResponse> getSeasonById(@PathVariable Long id);

    @Operation(summary = "Get all seasons")
    @GetMapping
    ResponseEntity<List<SeasonResponse>> getAllSeasons();

    @Operation(summary = "Get active season")
    @GetMapping("/active")
    ResponseEntity<SeasonResponse> getActiveSeason();

    @Operation(summary = "Update season")
    @PutMapping("/{id}")
    ResponseEntity<SeasonResponse> updateSeason(@PathVariable Long id, @Valid @RequestBody SeasonUpdateRequest request);

    @Operation(summary = "Delete season")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteSeason(@PathVariable Long id);

    @Operation(summary = "Update season status")
    @PatchMapping("/{id}/status")
    ResponseEntity<SeasonResponse> updateSeasonStatus(@PathVariable Long id, @RequestParam SeasonStatus status);
}