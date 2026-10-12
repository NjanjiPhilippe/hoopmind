package com.np3tech.hoopmind.nba.controller;

import com.np3tech.hoopmind.nba.api.GameApi;
import com.np3tech.hoopmind.nba.dto.request.GameCreateRequest;
import com.np3tech.hoopmind.nba.dto.request.GameUpdateRequest;
import com.np3tech.hoopmind.nba.dto.response.GameResponse;
import com.np3tech.hoopmind.nba.entity.Game;
import com.np3tech.hoopmind.nba.entity.GameResult;
import com.np3tech.hoopmind.nba.entity.enummeration.GameStatus;
import com.np3tech.hoopmind.nba.mapper.GameMapper;
import com.np3tech.hoopmind.nba.service.search.GameSearchService;
import com.np3tech.hoopmind.nba.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class GameController implements GameApi {

    private final GameService gameService;
    private final GameSearchService gameSearchService;
    private final GameMapper gameMapper;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GameResponse> createGame(GameCreateRequest request) {
        log.info("Creating game: {} vs {}", request.getHomeTeamId(), request.getAwayTeamId());

        Game game = gameMapper.toEntity(request);
        Game createdGame = gameService.createGame(game);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gameMapper.toResponse(createdGame));
    }

    @Override
    public ResponseEntity<GameResponse> getGameById(Long id) {
        log.debug("Fetching game with id: {}", id);

        Game game = gameService.getGameById(id);
        return ResponseEntity.ok(gameMapper.toResponse(game, gameService.getResults(List.of(game)).get(game.getId())));
    }

    @Override
    public ResponseEntity<List<GameResponse>> getAllGames() {
        log.debug("Fetching all games");

        List<Game> games = gameService.getAllGames();
        List<GameResponse> responses = toResponses(games);

        return ResponseEntity.ok(responses);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GameResponse> updateGame(Long id, GameUpdateRequest request) {
        log.info("Updating game with id: {}", id);

        Game gameUpdate = gameMapper.toEntity(request);
        Game updatedGame = gameService.updateGame(id, gameUpdate);

        return ResponseEntity.ok(gameMapper.toResponse(updatedGame));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteGame(Long id) {
        log.info("Deleting game with id: {}", id);

        gameService.deleteGame(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<GameResponse>> getGamesByStatus(GameStatus status) {
        log.debug("Fetching games by status: {}", status);

        List<Game> games = gameService.getGamesByStatus(status);
        List<GameResponse> responses = toResponses(games);

        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<List<GameResponse>> getGamesBySeason(Long seasonId) {
        log.debug("Fetching games for season: {}", seasonId);

        List<Game> games = gameService.getGamesBySeason(seasonId);
        List<GameResponse> responses = toResponses(games);

        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<List<GameResponse>> getGamesByTeam(Long teamId) {
        log.debug("Fetching games for team: {}", teamId);

        List<Game> games = gameService.getGamesByTeam(teamId);
        List<GameResponse> responses = toResponses(games);

        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<List<GameResponse>> getGamesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Fetching games between {} and {}", startDate, endDate);

        List<Game> games = gameService.getGamesByDateRange(startDate, endDate);
        List<GameResponse> responses = toResponses(games);

        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<List<GameResponse>> getTodaysGames() {
        log.debug("Fetching today's games");

        List<Game> games = gameSearchService.getTodaysGames();
        List<GameResponse> responses = toResponses(games);

        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<List<GameResponse>> getLiveGames() {
        log.debug("Fetching live games");

        List<Game> games = gameSearchService.getLiveGames();
        List<GameResponse> responses = toResponses(games);

        return ResponseEntity.ok(responses);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GameResponse> updateGameStatus(Long id, GameStatus status) {
        log.info("Updating game status for id: {} to {}", id, status);

        gameService.updateGameStatus(id, status);
        Game game = gameService.getGameById(id);

        return ResponseEntity.ok(gameMapper.toResponse(game));
    }

    /** Les scores des matchs joués sont chargés en une requête pour toute la liste. */
    private List<GameResponse> toResponses(List<Game> games) {
        Map<Long, GameResult> results = gameService.getResults(games);
        return games.stream().map(game -> gameMapper.toResponse(game, results.get(game.getId()))).toList();
    }
}
