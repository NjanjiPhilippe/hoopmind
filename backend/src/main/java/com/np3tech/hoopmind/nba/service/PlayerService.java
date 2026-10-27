package com.np3tech.hoopmind.nba.service;

import com.np3tech.hoopmind.nba.dto.request.PlayerCreateRequest;
import com.np3tech.hoopmind.nba.dto.request.PlayerUpdateRequest;
import com.np3tech.hoopmind.nba.entity.Player;
import com.np3tech.hoopmind.nba.repository.PlayerRepository;
import com.np3tech.hoopmind.nba.repository.PlayerStatsRepository;
import com.np3tech.hoopmind.shared.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final PlayerStatsRepository playerStatsRepository;
    private final TeamService teamService;

    public Player createPlayer(PlayerCreateRequest request) {
        log.info("Creating player: {} {}", request.getFirstName(), request.getLastName());
        return playerRepository.save(Player.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .position(request.getPosition())
                .jerseyNumber(request.getJerseyNumber())
                .team(teamService.getTeamById(request.getTeamId()))
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
                .build());
    }

    public Player updatePlayer(Long id, PlayerUpdateRequest request) {
        Player player = getPlayerById(id);
        if (request.getFirstName() != null) {
            player.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            player.setLastName(request.getLastName());
        }
        if (request.getPosition() != null) {
            player.setPosition(request.getPosition());
        }
        if (request.getJerseyNumber() != null) {
            player.setJerseyNumber(request.getJerseyNumber());
        }
        if (request.getTeamId() != null) {
            player.setTeam(teamService.getTeamById(request.getTeamId()));
        }
        if (request.getHeight() != null) {
            player.setHeight(request.getHeight());
        }
        if (request.getWeight() != null) {
            player.setWeight(request.getWeight());
        }
        if (request.getPhotoUrl() != null) {
            player.setPhotoUrl(request.getPhotoUrl());
        }
        if (request.getStatus() != null) {
            player.setStatus(request.getStatus());
        }
        return playerRepository.save(player);
    }

    @Transactional(readOnly = true)
    public Player getPlayerById(Long id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("PLAYER_NOT_FOUND", "Player not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Player> getAllPlayers(Pageable pageable) {
        return playerRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<Player> getPlayersByTeam(Long teamId) {
        return playerRepository.findByTeam(teamService.getTeamById(teamId));
    }

    public void deletePlayer(Long id) {
        log.info("Deleting player with id: {}", id);
        Player player = getPlayerById(id);
        playerStatsRepository.deleteByPlayer(player);
        playerRepository.delete(player);
    }
}
