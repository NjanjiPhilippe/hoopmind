package com.np3tech.hoopmind.nba.service.impl;

import com.np3tech.hoopmind.nba.entity.Player;
import com.np3tech.hoopmind.nba.entity.PlayerStats;
import com.np3tech.hoopmind.nba.entity.Season;
import com.np3tech.hoopmind.shared.exception.AppException;
import com.np3tech.hoopmind.nba.repository.PlayerStatsRepository;
import com.np3tech.hoopmind.nba.service.PlayerService;
import com.np3tech.hoopmind.nba.service.PlayerStatsService;
import com.np3tech.hoopmind.nba.service.SeasonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PlayerStatsServiceImpl implements PlayerStatsService {

    private final PlayerStatsRepository playerStatsRepository;
    private final PlayerService playerService;
    private final SeasonService seasonService;

    public PlayerStats createOrUpdatePlayerStats(PlayerStats playerStats) {
        log.info("Creating/Updating player stats for player id: {} and season id: {}",
                playerStats.getPlayer().getId(), playerStats.getSeason().getId());

        validatePlayerStats(playerStats);

        playerStats.setLastUpdated(LocalDateTime.now());

        return playerStatsRepository.findByPlayerAndSeason(playerStats.getPlayer(), playerStats.getSeason())
                .map(existing -> {
                    updateExistingStats(existing, playerStats);
                    return playerStatsRepository.save(existing);
                })
                .orElseGet(() -> playerStatsRepository.save(playerStats));
    }

    @Transactional(readOnly = true)
    public PlayerStats getPlayerStatsById(Long id) {
        return playerStatsRepository.findById(id)
                .orElseThrow(() -> new AppException(
                        "Player stats not found with id: " + id,
                        HttpStatus.NOT_FOUND,
                        "PLAYER_STATS_NOT_FOUND"
                ));
    }

    @Transactional(readOnly = true)
    public PlayerStats getPlayerStatsByPlayerAndSeason(Long playerId, Long seasonId) {
        Player player = playerService.getPlayerById(playerId);
        Season season = seasonService.getSeasonById(seasonId);

        return playerStatsRepository.findByPlayerAndSeason(player, season)
                .orElseThrow(() -> new AppException(
                        "Player stats not found for player id: " + playerId + " and season id: " + seasonId,
                        HttpStatus.NOT_FOUND,
                        "PLAYER_STATS_NOT_FOUND"
                ));
    }

    @Transactional(readOnly = true)
    public List<PlayerStats> getPlayerStatsBySeason(Long seasonId) {
        Season season = seasonService.getSeasonById(seasonId);
        return playerStatsRepository.findBySeason(season);
    }

    @Transactional(readOnly = true)
    public List<PlayerStats> getTopScorers(Long seasonId) {
        Season season = seasonService.getSeasonById(seasonId);
        return playerStatsRepository.findTopScorers(season);
    }

    @Transactional(readOnly = true)
    public List<PlayerStats> getTopAssists(Long seasonId) {
        Season season = seasonService.getSeasonById(seasonId);
        return playerStatsRepository.findTopAssists(season);
    }

    @Transactional(readOnly = true)
    public List<PlayerStats> getTopRebounders(Long seasonId) {
        Season season = seasonService.getSeasonById(seasonId);
        return playerStatsRepository.findTopRebounders(season);
    }

    public void deletePlayerStats(Long id) {
        log.info("Deleting player stats with id: {}", id);

        if (!playerStatsRepository.existsById(id)) {
            throw new AppException(
                    "Player stats not found with id: " + id,
                    HttpStatus.NOT_FOUND,
                    "PLAYER_STATS_NOT_FOUND"
            );
        }

        playerStatsRepository.deleteById(id);
    }

    private void validatePlayerStats(PlayerStats playerStats) {
        if (playerStats.getPlayer() == null) {
            throw new AppException(
                    "Player is required for player stats",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PLAYER_STATS_DATA"
            );
        }

        if (playerStats.getSeason() == null) {
            throw new AppException(
                    "Season is required for player stats",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PLAYER_STATS_DATA"
            );
        }
    }

    private void updateExistingStats(PlayerStats existing, PlayerStats updated) {
        if (updated.getGamesPlayed() != null) existing.setGamesPlayed(updated.getGamesPlayed());
        if (updated.getGamesStarted() != null) existing.setGamesStarted(updated.getGamesStarted());
        if (updated.getPointsPerGame() != null) existing.setPointsPerGame(updated.getPointsPerGame());
        if (updated.getAssistsPerGame() != null) existing.setAssistsPerGame(updated.getAssistsPerGame());
        if (updated.getTotalReboundsPerGame() != null) existing.setTotalReboundsPerGame(updated.getTotalReboundsPerGame());
        if (updated.getFieldGoalPercentage() != null) existing.setFieldGoalPercentage(updated.getFieldGoalPercentage());
        if (updated.getThreePointPercentage() != null) existing.setThreePointPercentage(updated.getThreePointPercentage());
        existing.setLastUpdated(LocalDateTime.now());
    }
}