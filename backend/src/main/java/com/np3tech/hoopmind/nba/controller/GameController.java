package com.np3tech.hoopmind.nba.controller;

import com.np3tech.hoopmind.nba.dto.request.GameCreateRequest;
import com.np3tech.hoopmind.nba.dto.request.GameResultRequest;
import com.np3tech.hoopmind.nba.dto.request.GameUpdateRequest;
import com.np3tech.hoopmind.nba.dto.response.GameResponse;
import com.np3tech.hoopmind.nba.entity.Game;
import com.np3tech.hoopmind.nba.entity.GameResult;
import com.np3tech.hoopmind.nba.mapper.GameMapper;
import com.np3tech.hoopmind.nba.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/games")
@RequiredArgsConstructor
@Tag(name = "Games", description = "Game management APIs")
class GameController {

    private final GameService gameService;
    private final GameMapper gameMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(operationId = "createGame", summary = "Create a new game")
    GameResponse createGame(@Valid @RequestBody GameCreateRequest request) {
        return gameMapper.toResponse(gameService.createGame(request));
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getGameById", summary = "Get game by ID")
    GameResponse getGameById(@Parameter(description = "Game ID") @PathVariable Long id) {
        return toResponses(List.of(gameService.getGameById(id))).getFirst();
    }

    @GetMapping
    @Operation(operationId = "getAllGames", summary = "Get all games", description = "Paginated, most recent first (at most 100 per page)")
    PagedModel<GameResponse> getAllGames(@ParameterObject @PageableDefault(size = 20, sort = "scheduledDate",
            direction = Sort.Direction.DESC) Pageable pageable) {
        var games = gameService.getAllGames(pageable);
        Map<Long, GameResult> results = gameService.getResults(games.getContent());
        return new PagedModel<>(games.map(game -> gameMapper.toResponse(game, results.get(game.getId()))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(operationId = "updateGame", summary = "Update game")
    GameResponse updateGame(@Parameter(description = "Game ID") @PathVariable Long id,
                            @Valid @RequestBody GameUpdateRequest request) {
        return gameMapper.toResponse(gameService.updateGame(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(operationId = "deleteGame", summary = "Delete game")
    void deleteGame(@Parameter(description = "Game ID") @PathVariable Long id) {
        gameService.deleteGame(id);
    }

    @GetMapping("/season/{seasonId}")
    @Operation(operationId = "getGamesBySeason", summary = "Get games by season")
    List<GameResponse> getGamesBySeason(@Parameter(description = "Season ID") @PathVariable Long seasonId) {
        return toResponses(gameService.getGamesBySeason(seasonId));
    }

    @PutMapping("/{id}/result")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(operationId = "recordGameResult", summary = "Enregistrer le score d'un match",
            description = "Saisie manuelle par un administrateur, quand l'import n'est pas disponible. Crée ou corrige "
                    + "le score final et marque le match comme terminé ; la forme des équipes en tient compte dès la "
                    + "prédiction suivante. 400 si le score est invalide (négatif, égalité…).")
    GameResponse recordGameResult(@Parameter(description = "Game ID") @PathVariable Long id,
                                  @Valid @RequestBody GameResultRequest request) {
        GameResult result = gameService.recordResult(id, request);
        return gameMapper.toResponse(gameService.getGameById(id), result);
    }

    @DeleteMapping("/{id}/result")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(operationId = "deleteGameResult", summary = "Retirer le score d'un match", description = "Le match redevient « à venir ».")
    void deleteGameResult(@Parameter(description = "Game ID") @PathVariable Long id) {
        gameService.deleteResult(id);
    }

    /** Les scores des matchs joués sont chargés en une requête pour toute la liste. */
    private List<GameResponse> toResponses(Collection<Game> games) {
        Map<Long, GameResult> results = gameService.getResults(games);
        return games.stream().map(game -> gameMapper.toResponse(game, results.get(game.getId()))).toList();
    }
}
