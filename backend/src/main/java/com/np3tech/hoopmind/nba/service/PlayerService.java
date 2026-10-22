package com.np3tech.hoopmind.nba.service;

import com.np3tech.hoopmind.nba.dto.request.PlayerCreateRequest;
import com.np3tech.hoopmind.nba.dto.request.PlayerUpdateRequest;
import com.np3tech.hoopmind.nba.entity.Player;
import com.np3tech.hoopmind.nba.entity.Team;
import com.np3tech.hoopmind.nba.entity.enummeration.PlayerStatus;
import com.np3tech.hoopmind.nba.entity.enummeration.Position;
import com.np3tech.hoopmind.shared.exception.AppException;
import com.np3tech.hoopmind.nba.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final TeamService teamService;

    public Player createPlayer(PlayerCreateRequest request) {
        Team team = teamService.getTeamById(request.getTeamId());

        Player player = Player.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .position(request.getPosition())
                .jerseyNumber(request.getJerseyNumber())
                .team(team)
                .birthDate(request.getBirthDate())
                .birthPlace(request.getBirthPlace())
                .nationality(request.getNationality())
                .height(request.getHeight())
                .weight(request.getWeight())
                .experience(request.getExperience())
                .college(request.getCollege())
                .draftYear(request.getDraftYear())
                .draftRound(request.getDraftRound())
                .draftNumber(request.getDraftNumber())
                .photoUrl(request.getPhotoUrl())
                .build();

        log.info("Creating player: {} {}", player.getFirstName(), player.getLastName());

        validatePlayer(player);

        return playerRepository.save(player);
    }

    public Player updatePlayer(Long id, PlayerUpdateRequest request) {
        log.info("Updating player with id: {}", id);

        Player existingPlayer = getPlayerById(id);

        if (request.getFirstName() != null) {
            existingPlayer.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            existingPlayer.setLastName(request.getLastName());
        }
        if (request.getPosition() != null) {
            existingPlayer.setPosition(request.getPosition());
        }
        if (request.getJerseyNumber() != null) {
            existingPlayer.setJerseyNumber(request.getJerseyNumber());
        }
        if (request.getTeamId() != null) {
            existingPlayer.setTeam(teamService.getTeamById(request.getTeamId()));
        }
        if (request.getHeight() != null) {
            existingPlayer.setHeight(request.getHeight());
        }
        if (request.getWeight() != null) {
            existingPlayer.setWeight(request.getWeight());
        }
        if (request.getPhotoUrl() != null) {
            existingPlayer.setPhotoUrl(request.getPhotoUrl());
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
    public Page<Player> getAllPlayers(Pageable pageable) {
        return playerRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<Player> getPlayersByTeam(Long teamId) {
        Team team = teamService.getTeamById(teamId);
        return playerRepository.findByTeam(team);
    }

    @Transactional(readOnly = true)
    public Page<Player> getPlayersByPosition(Position position, Pageable pageable) {
        return playerRepository.findByPosition(position, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Player> getPlayersByStatus(PlayerStatus status, Pageable pageable) {
        return playerRepository.findByStatus(status, pageable);
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