package com.np3tech.hoopmind.nba.service.impl;

import com.np3tech.hoopmind.nba.entity.Player;
import com.np3tech.hoopmind.nba.entity.Team;
import com.np3tech.hoopmind.nba.entity.enummeration.PlayerStatus;
import com.np3tech.hoopmind.nba.entity.enummeration.Position;
import com.np3tech.hoopmind.shared.exception.AppException;
import com.np3tech.hoopmind.nba.repository.PlayerRepository;
import com.np3tech.hoopmind.nba.service.PlayerService;
import com.np3tech.hoopmind.nba.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerServiceImpl implements PlayerService {

    private final PlayerRepository playerRepository;
    private final TeamService teamService;

    public Player createPlayer(Player player) {
        log.info("Creating player: {} {}", player.getFirstName(), player.getLastName());

        validatePlayer(player);

        return playerRepository.save(player);
    }

    public Player updatePlayer(Long id, Player playerUpdate) {
        log.info("Updating player with id: {}", id);

        Player existingPlayer = getPlayerById(id);

        if (playerUpdate.getFirstName() != null) {
            existingPlayer.setFirstName(playerUpdate.getFirstName());
        }
        if (playerUpdate.getLastName() != null) {
            existingPlayer.setLastName(playerUpdate.getLastName());
        }
        if (playerUpdate.getPosition() != null) {
            existingPlayer.setPosition(playerUpdate.getPosition());
        }
        if (playerUpdate.getJerseyNumber() != null) {
            existingPlayer.setJerseyNumber(playerUpdate.getJerseyNumber());
        }
        if (playerUpdate.getTeam() != null) {
            existingPlayer.setTeam(playerUpdate.getTeam());
        }
        if (playerUpdate.getStatus() != null) {
            existingPlayer.setStatus(playerUpdate.getStatus());
        }
        if (playerUpdate.getHeight() != null) {
            existingPlayer.setHeight(playerUpdate.getHeight());
        }
        if (playerUpdate.getWeight() != null) {
            existingPlayer.setWeight(playerUpdate.getWeight());
        }
        if (playerUpdate.getPhotoUrl() != null) {
            existingPlayer.setPhotoUrl(playerUpdate.getPhotoUrl());
        }

        return playerRepository.save(existingPlayer);
    }

    @Transactional(readOnly = true)
    public Player getPlayerById(Long id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> new AppException(
                        "Player not found with id: " + id,
                        HttpStatus.NOT_FOUND,
                        "PLAYER_NOT_FOUND"
                ));
    }

    @Transactional(readOnly = true)
    public List<Player> getAllPlayers() {
        return playerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Player> getPlayersByTeam(Long teamId) {
        Team team = teamService.getTeamById(teamId);
        return playerRepository.findByTeam(team);
    }

    @Transactional(readOnly = true)
    public List<Player> getPlayersByPosition(Position position) {
        return playerRepository.findByPosition(position);
    }

    @Transactional(readOnly = true)
    public List<Player> getPlayersByStatus(PlayerStatus status) {
        return playerRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<Player> searchPlayersByName(String name) {
        return playerRepository.searchByName(name);
    }

    public void updatePlayerStatus(Long id, PlayerStatus status) {
        log.info("Updating player status for id: {} to {}", id, status);

        Player player = getPlayerById(id);
        player.setStatus(status);

        playerRepository.save(player);
    }

    public void deletePlayer(Long id) {
        log.info("Deleting player with id: {}", id);

        if (!playerRepository.existsById(id)) {
            throw new AppException(
                    "Player not found with id: " + id,
                    HttpStatus.NOT_FOUND,
                    "PLAYER_NOT_FOUND"
            );
        }

        playerRepository.deleteById(id);
    }

    private void validatePlayer(Player player) {
        if (player.getFirstName() == null || player.getLastName() == null) {
            throw new AppException(
                    "Player first name and last name are required",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PLAYER_DATA"
            );
        }

        if (player.getTeam() == null) {
            throw new AppException(
                    "Player team is required",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PLAYER_DATA"
            );
        }
    }
}