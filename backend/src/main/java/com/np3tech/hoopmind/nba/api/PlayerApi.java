package com.np3tech.hoopmind.nba.api;

import com.np3tech.hoopmind.nba.dto.request.PlayerCreateRequest;
import com.np3tech.hoopmind.nba.dto.request.PlayerUpdateRequest;
import com.np3tech.hoopmind.nba.dto.response.PlayerResponse;
import com.np3tech.hoopmind.nba.entity.enummeration.PlayerStatus;
import com.np3tech.hoopmind.nba.entity.enummeration.Position;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "Players", description = "Player management APIs")
@RequestMapping("/api/v1/players")
public interface PlayerApi {

    @Operation(summary = "Create a new player")
    @PostMapping
    ResponseEntity<PlayerResponse> createPlayer(@Valid @RequestBody PlayerCreateRequest request);

    @Operation(summary = "Get player by ID")
    @GetMapping("/{id}")
    ResponseEntity<PlayerResponse> getPlayerById(@PathVariable Long id);

    @Operation(summary = "Get all players")
    @GetMapping
    ResponseEntity<PagedModel<PlayerResponse>> getAllPlayers(@ParameterObject @PageableDefault(size = 20, sort = "lastName") Pageable pageable);

    @Operation(summary = "Update player")
    @PutMapping("/{id}")
    ResponseEntity<PlayerResponse> updatePlayer(@PathVariable Long id, @Valid @RequestBody PlayerUpdateRequest request);

    @Operation(summary = "Delete player")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deletePlayer(@PathVariable Long id);

    @Operation(summary = "Get players by team")
    @GetMapping("/team/{teamId}")
    ResponseEntity<List<PlayerResponse>> getPlayersByTeam(@PathVariable Long teamId);

    @Operation(summary = "Get players by position")
    @GetMapping("/position/{position}")
    ResponseEntity<PagedModel<PlayerResponse>> getPlayersByPosition(@PathVariable Position position,
                                                                @ParameterObject @PageableDefault(size = 20, sort = "lastName") Pageable pageable);

    @Operation(summary = "Get players by status")
    @GetMapping("/status/{status}")
    ResponseEntity<PagedModel<PlayerResponse>> getPlayersByStatus(@PathVariable PlayerStatus status,
                                                              @ParameterObject @PageableDefault(size = 20, sort = "lastName") Pageable pageable);

    @Operation(summary = "Search players by name")
    @GetMapping("/search")
    ResponseEntity<List<PlayerResponse>> searchPlayersByName(@RequestParam String name);

    @Operation(summary = "Update player status")
    @PatchMapping("/{id}/status")
    ResponseEntity<PlayerResponse> updatePlayerStatus(@PathVariable Long id, @RequestParam PlayerStatus status);
}
