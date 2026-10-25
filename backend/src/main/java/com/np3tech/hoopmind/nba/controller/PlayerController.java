package com.np3tech.hoopmind.nba.controller;

import com.np3tech.hoopmind.nba.api.PlayerApi;
import com.np3tech.hoopmind.nba.mapper.PlayerMapper;
import com.np3tech.hoopmind.nba.dto.request.PlayerCreateRequest;
import com.np3tech.hoopmind.nba.dto.request.PlayerUpdateRequest;
import com.np3tech.hoopmind.nba.dto.response.PlayerResponse;
import com.np3tech.hoopmind.nba.entity.Player;
import com.np3tech.hoopmind.nba.entity.enummeration.PlayerStatus;
import com.np3tech.hoopmind.nba.entity.enummeration.Position;
import com.np3tech.hoopmind.nba.service.PlayerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PlayerController implements PlayerApi {

    private final PlayerService playerService;
    private final PlayerMapper playerMapper;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlayerResponse> createPlayer(PlayerCreateRequest request) {
        log.info("Creating player: {} {}", request.getFirstName(), request.getLastName());
        Player createdPlayer = playerService.createPlayer(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(playerMapper.toResponse(createdPlayer));
    }

    @Override
    public ResponseEntity<PlayerResponse> getPlayerById(Long id) {
        log.debug("Fetching player with id: {}", id);

        Player player = playerService.getPlayerById(id);
        return ResponseEntity.ok(playerMapper.toResponse(player));
    }

    @Override
    public ResponseEntity<PagedModel<PlayerResponse>> getAllPlayers(Pageable pageable) {
        return ResponseEntity.ok(new PagedModel<>(playerService.getAllPlayers(pageable).map(playerMapper::toResponse)));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlayerResponse> updatePlayer(Long id, PlayerUpdateRequest request) {
        log.info("Updating player with id: {}", id);
        Player updatedPlayer = playerService.updatePlayer(id, request);

        return ResponseEntity.ok(playerMapper.toResponse(updatedPlayer));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePlayer(Long id) {
        log.info("Deleting player with id: {}", id);

        playerService.deletePlayer(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<PlayerResponse>> getPlayersByTeam(Long teamId) {
        log.debug("Fetching players for team: {}", teamId);

        List<Player> players = playerService.getPlayersByTeam(teamId);
        List<PlayerResponse> responses = players.stream()
                .map(playerMapper::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<PagedModel<PlayerResponse>> getPlayersByPosition(Position position, Pageable pageable) {
        return ResponseEntity.ok(new PagedModel<>(playerService.getPlayersByPosition(position, pageable).map(playerMapper::toResponse)));
    }

    @Override
    public ResponseEntity<PagedModel<PlayerResponse>> getPlayersByStatus(PlayerStatus status, Pageable pageable) {
        return ResponseEntity.ok(new PagedModel<>(playerService.getPlayersByStatus(status, pageable).map(playerMapper::toResponse)));
    }

    @Override
    public ResponseEntity<List<PlayerResponse>> searchPlayersByName(String name) {
        log.debug("Searching players by name: {}", name);

        List<Player> players = playerService.searchPlayersByName(name);
        List<PlayerResponse> responses = players.stream()
                .map(playerMapper::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlayerResponse> updatePlayerStatus(Long id, PlayerStatus status) {
        log.info("Updating player status for id: {} to {}", id, status);

        playerService.updatePlayerStatus(id, status);
        Player player = playerService.getPlayerById(id);

        return ResponseEntity.ok(playerMapper.toResponse(player));
    }
}
