package com.np3tech.hoopmind.nba.controller;

import com.np3tech.hoopmind.nba.dto.request.SeasonCreateRequest;
import com.np3tech.hoopmind.nba.dto.request.SeasonUpdateRequest;
import com.np3tech.hoopmind.nba.dto.response.SeasonResponse;
import com.np3tech.hoopmind.nba.mapper.SeasonMapper;
import com.np3tech.hoopmind.nba.service.SeasonService;
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
@RequestMapping("/api/v1/seasons")
@RequiredArgsConstructor
@Tag(name = "Seasons", description = "Season management APIs")
class SeasonController {

    private final SeasonService seasonService;
    private final SeasonMapper seasonMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(operationId = "createSeason", summary = "Create a new season")
    SeasonResponse createSeason(@Valid @RequestBody SeasonCreateRequest request) {
        return seasonMapper.toResponse(seasonService.createSeason(request));
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getSeasonById", summary = "Get season by ID")
    SeasonResponse getSeasonById(@PathVariable Long id) {
        return seasonMapper.toResponse(seasonService.getSeasonById(id));
    }

    @GetMapping
    @Operation(operationId = "getAllSeasons", summary = "Get all seasons", description = "Most recent first")
    List<SeasonResponse> getAllSeasons() {
        return seasonService.getAllSeasons().stream().map(seasonMapper::toResponse).toList();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(operationId = "updateSeason", summary = "Update season")
    SeasonResponse updateSeason(@PathVariable Long id, @Valid @RequestBody SeasonUpdateRequest request) {
        return seasonMapper.toResponse(seasonService.updateSeason(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(operationId = "deleteSeason", summary = "Delete season")
    void deleteSeason(@PathVariable Long id) {
        seasonService.deleteSeason(id);
    }
}
