package com.np3tech.hoopmind.nba.api;

import com.np3tech.hoopmind.nba.dto.request.GameCreateRequest;
import com.np3tech.hoopmind.nba.dto.request.GameUpdateRequest;
import com.np3tech.hoopmind.nba.dto.response.GameResponse;
import com.np3tech.hoopmind.nba.entity.enummeration.GameStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Games", description = "Game management APIs")
@RequestMapping("/api/v1/games")
public interface GameApi {

    @Operation(summary = "Create a new game", description = "Creates a new game in the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Game created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "409", description = "Game already exists")
    })
    @PostMapping
    ResponseEntity<GameResponse> createGame(@Valid @RequestBody GameCreateRequest request);

    @Operation(summary = "Get game by ID", description = "Retrieves a game by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Game found"),
            @ApiResponse(responseCode = "404", description = "Game not found")
    })
    @GetMapping("/{id}")
    ResponseEntity<GameResponse> getGameById(
            @Parameter(description = "Game ID") @PathVariable Long id
    );

    @Operation(summary = "Get all games", description = "Paginated, most recent first (at most 100 per page)")
    @GetMapping
    ResponseEntity<PagedModel<GameResponse>> getAllGames(@ParameterObject @PageableDefault(size = 20, sort = "scheduledDate", direction = Sort.Direction.DESC) Pageable pageable);

    @Operation(summary = "Update game", description = "Updates an existing game")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Game updated successfully"),
            @ApiResponse(responseCode = "404", description = "Game not found")
    })
    @PutMapping("/{id}")
    ResponseEntity<GameResponse> updateGame(
            @Parameter(description = "Game ID") @PathVariable Long id,
            @Valid @RequestBody GameUpdateRequest request
    );

    @Operation(summary = "Delete game", description = "Deletes a game by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Game deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Game not found")
    })
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteGame(@Parameter(description = "Game ID") @PathVariable Long id);

    @Operation(summary = "Get games by status", description = "Retrieves games filtered by status")
    @GetMapping("/status/{status}")
    ResponseEntity<PagedModel<GameResponse>> getGamesByStatus(
            @Parameter(description = "Game status") @PathVariable GameStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = "scheduledDate", direction = Sort.Direction.DESC) Pageable pageable
    );

    @Operation(summary = "Get games by season", description = "Retrieves games for a specific season")
    @GetMapping("/season/{seasonId}")
    ResponseEntity<List<GameResponse>> getGamesBySeason(
            @Parameter(description = "Season ID") @PathVariable Long seasonId
    );

    @Operation(summary = "Get games by team", description = "Retrieves games for a specific team")
    @GetMapping("/team/{teamId}")
    ResponseEntity<PagedModel<GameResponse>> getGamesByTeam(
            @Parameter(description = "Team ID") @PathVariable Long teamId,
            @ParameterObject @PageableDefault(size = 20, sort = "scheduledDate", direction = Sort.Direction.DESC) Pageable pageable
    );

    @Operation(summary = "Get games by date range", description = "Retrieves games within a date range of at most 31 days")
    @GetMapping("/date-range")
    ResponseEntity<List<GameResponse>> getGamesByDateRange(
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    );

    @Operation(summary = "Get today's games", description = "Retrieves games scheduled for today")
    @GetMapping("/today")
    ResponseEntity<List<GameResponse>> getTodaysGames();

    @Operation(summary = "Get live games", description = "Retrieves games currently in progress")
    @GetMapping("/live")
    ResponseEntity<List<GameResponse>> getLiveGames();

    @Operation(summary = "Update game status", description = "Updates the status of a game")
    @PatchMapping("/{id}/status")
    ResponseEntity<GameResponse> updateGameStatus(
            @Parameter(description = "Game ID") @PathVariable Long id,
            @Parameter(description = "New status") @RequestParam GameStatus status
    );
}
