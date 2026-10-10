package com.np3tech.hoopmind.nba.service.impl;

import com.np3tech.hoopmind.nba.entity.Game;
import com.np3tech.hoopmind.nba.entity.Player;
import com.np3tech.hoopmind.nba.entity.PlayerGameStats;
import com.np3tech.hoopmind.shared.exception.AppException;
import com.np3tech.hoopmind.nba.repository.PlayerGameStatsRepository;
import com.np3tech.hoopmind.nba.service.GameService;
import com.np3tech.hoopmind.nba.service.PlayerGameStatsService;
import com.np3tech.hoopmind.nba.service.PlayerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PlayerGameStatsServiceImpl implements PlayerGameStatsService {

    private final PlayerGameStatsRepository playerGameStatsRepository;
    private final PlayerService playerService;
    private final GameService gameService;

    public PlayerGameStats createPlayerGameStats(PlayerGameStats stats) {
        log.info("Creating player game stats for player id: {} and game id: {}",
                stats.getPlayer().getId(), stats.getGame().getId());

        validatePlayerGameStats(stats);

        // Check if stats already exist
        playerGameStatsRepository.findByPlayerAndGame(stats.getPlayer(), stats.getGame())
                .ifPresent(existing -> {
                    throw new AppException(
                            "Player game stats already exist for player id: " + stats.getPlayer().getId() +
                                    " and game id: " + stats.getGame().getId(),
                            HttpStatus.CONFLICT,
                            "PLAYER_GAME_STATS_ALREADY_EXIST"
                    );
                });

        return playerGameStatsRepository.save(stats);
    }

    public PlayerGameStats updatePlayerGameStats(Long id, PlayerGameStats statsUpdate) {
        log.info("Updating player game stats with id: {}", id);

        PlayerGameStats existing = getPlayerGameStatsById(id);

        if (statsUpdate.getPoints() != null) existing.setPoints(statsUpdate.getPoints());
        if (statsUpdate.getAssists() != null) existing.setAssists(statsUpdate.getAssists());
        if (statsUpdate.getTotalRebounds() != null) existing.setTotalRebounds(statsUpdate.getTotalRebounds());
        if (statsUpdate.getMinutesPlayed() != null) existing.setMinutesPlayed(statsUpdate.getMinutesPlayed());
        if (statsUpdate.getFieldGoalsMade() != null) existing.setFieldGoalsMade(statsUpdate.getFieldGoalsMade());
        if (statsUpdate.getFieldGoalsAttempted() != null) existing.setFieldGoalsAttempted(statsUpdate.getFieldGoalsAttempted());

        return playerGameStatsRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public PlayerGameStats getPlayerGameStatsById(Long id) {
        return playerGameStatsRepository.findById(id)
                .orElseThrow(() -> new AppException(
                        "Player game stats not found with id: " + id,
                        HttpStatus.NOT_FOUND,
                        "PLAYER_GAME_STATS_NOT_FOUND"
                ));
    }

    @Transactional(readOnly = true)
    public PlayerGameStats getPlayerGameStatsByPlayerAndGame(Long playerId, Long gameId) {
        Player player = playerService.getPlayerById(playerId);
        Game game = gameService.getGameById(gameId);

        return playerGameStatsRepository.findByPlayerAndGame(player, game)
                .orElseThrow(() -> new AppException(
                        "Player game stats not found for player id: " + playerId + " and game id: " + gameId,
                        HttpStatus.NOT_FOUND,
                        "PLAYER_GAME_STATS_NOT_FOUND"
                ));
    }

    @Transactional(readOnly = true)
    public List<PlayerGameStats> getPlayerGameStatsByGame(Long gameId) {
        Game game = gameService.getGameById(gameId);
        return playerGameStatsRepository.findByGame(game);
    }

    @Transactional(readOnly = true)
    public List<PlayerGameStats> getPlayerGameStatsByPlayer(Long playerId) {
        Player player = playerService.getPlayerById(playerId);
        return playerGameStatsRepository.findByPlayerOrderByDateDesc(player);
    }

    public void deletePlayerGameStats(Long id) {
        log.info("Deleting player game stats with id: {}", id);

        if (!playerGameStatsRepository.existsById(id)) {
            throw new AppException(
                    "Player game stats not found with id: " + id,
                    HttpStatus.NOT_FOUND,
                    "PLAYER_GAME_STATS_NOT_FOUND"
            );
        }

        playerGameStatsRepository.deleteById(id);
    }

    private void validatePlayerGameStats(PlayerGameStats stats) {
        if (stats.getPlayer() == null) {
            throw new AppException(
                    "Player is required for player game stats",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PLAYER_GAME_STATS_DATA"
            );
        }

        if (stats.getGame() == null) {
            throw new AppException(
                    "Game is required for player game stats",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PLAYER_GAME_STATS_DATA"
            );
        }
    }
}