package com.np3tech.hoopmind.nba.controller;

import com.np3tech.hoopmind.nba.dto.request.PlayerCreateRequest;
import com.np3tech.hoopmind.nba.dto.request.PlayerUpdateRequest;
import com.np3tech.hoopmind.nba.dto.response.PlayerResponse;
import com.np3tech.hoopmind.nba.mapper.PlayerMapper;
import com.np3tech.hoopmind.nba.service.PlayerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
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
@RequestMapping("/api/v1/players")
@RequiredArgsConstructor
@Tag(name = "Players", description = "Player management APIs")
class PlayerController {

    private final PlayerService playerService;
    private final PlayerMapper playerMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(operationId = "createPlayer", summary = "Create a new player")
    PlayerResponse createPlayer(@Valid @RequestBody PlayerCreateRequest request) {
        return playerMapper.toResponse(playerService.createPlayer(request));
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getPlayerById", summary = "Get player by ID")
    PlayerResponse getPlayerById(@PathVariable Long id) {
        return playerMapper.toResponse(playerService.getPlayerById(id));
    }

    @GetMapping
    @Operation(operationId = "getAllPlayers", summary = "Get all players", description = "Paginated, by last name (at most 100 per page)")
    PagedModel<PlayerResponse> getAllPlayers(@ParameterObject @PageableDefault(size = 20, sort = "lastName") Pageable pageable) {
        return new PagedModel<>(playerService.getAllPlayers(pageable).map(playerMapper::toResponse));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(operationId = "updatePlayer", summary = "Update player")
    PlayerResponse updatePlayer(@PathVariable Long id, @Valid @RequestBody PlayerUpdateRequest request) {
        return playerMapper.toResponse(playerService.updatePlayer(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(operationId = "deletePlayer", summary = "Delete player")
    void deletePlayer(@PathVariable Long id) {
        playerService.deletePlayer(id);
    }

    @GetMapping("/team/{teamId}")
    @Operation(operationId = "getPlayersByTeam", summary = "Get players by team")
    List<PlayerResponse> getPlayersByTeam(@PathVariable Long teamId) {
        return playerService.getPlayersByTeam(teamId).stream().map(playerMapper::toResponse).toList();
    }
}
