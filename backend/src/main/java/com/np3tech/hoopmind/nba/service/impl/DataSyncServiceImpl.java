package com.np3tech.hoopmind.nba.service.impl;

import com.np3tech.hoopmind.ml.entity.*;
import com.np3tech.hoopmind.nba.entity.*;
import com.np3tech.hoopmind.nba.entity.enummeration.GameStatus;
import com.np3tech.hoopmind.ml.repository.*;
import com.np3tech.hoopmind.nba.repository.*;
import com.np3tech.hoopmind.nba.service.DataSyncService;
import com.np3tech.hoopmind.ml.service.MatchPredictionService;
import com.np3tech.hoopmind.nba.service.SeasonService;
import com.np3tech.hoopmind.nba.service.StatisticsCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for synchronizing and updating statistics across the system
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DataSyncServiceImpl implements DataSyncService {

    private final GameRepository gameRepository;
    private final GameResultRepository gameResultRepository;
    private final MatchPredictionRepository matchPredictionRepository;
    private final StatisticsCalculationService statisticsCalculationService;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final SeasonService seasonService;
    private final MatchPredictionService matchPredictionService;

    /**
     * Update all statistics for a completed game
     */
    @Async
    public void updateGameStatistics(Long gameId) {
        log.info("Updating statistics for game id: {}", gameId);

        try {
            Game game = gameRepository.findById(gameId).orElseThrow();

            if (game.getStatus() != GameStatus.COMPLETED) {
                log.warn("Cannot update statistics for unfinished game id: {}", gameId);
                return;
            }

            // Update team season statistics
            statisticsCalculationService.calculateTeamSeasonStats(game.getHomeTeam(), game.getSeason());
            statisticsCalculationService.calculateTeamSeasonStats(game.getAwayTeam(), game.getSeason());

            log.info("Successfully updated statistics for game id: {}", gameId);
        } catch (Exception e) {
            log.error("Error updating statistics for game id: {}", gameId, e);
        }
    }

    /**
     * Evaluate all predictions for a completed game
     */
    @Async
    public void evaluateGamePredictions(Long gameId) {
        log.info("Evaluating predictions for game id: {}", gameId);

        try {
            Game game = gameRepository.findById(gameId).orElseThrow();

            List<MatchPrediction> predictions = matchPredictionRepository.findByGameOrderByDateDesc(game);

            for (MatchPrediction prediction : predictions) {
                if (prediction.getIsAccurate() == null) {
                    matchPredictionService.evaluatePrediction(prediction.getId());
                }
            }

            log.info("Successfully evaluated {} predictions for game id: {}", predictions.size(), gameId);
        } catch (Exception e) {
            log.error("Error evaluating predictions for game id: {}", gameId, e);
        }
    }

    /**
     * Recalculate all player statistics for a season
     */
    @Async
    public void recalculatePlayerSeasonStats(Long seasonId) {
        log.info("Recalculating player statistics for season id: {}", seasonId);

        try {
            Season season = seasonService.getSeasonById(seasonId);
            List<Player> players = playerRepository.findAll();

            int updated = 0;
            for (Player player : players) {
                try {
                    statisticsCalculationService.calculatePlayerSeasonStats(player, season);
                    updated++;
                } catch (Exception e) {
                    log.error("Error calculating stats for player id: {}", player.getId(), e);
                }
            }

            log.info("Successfully recalculated statistics for {} players", updated);
        } catch (Exception e) {
            log.error("Error recalculating player season stats for season id: {}", seasonId, e);
        }
    }

    /**
     * Recalculate all team statistics for a season
     */
    @Async
    public void recalculateTeamSeasonStats(Long seasonId) {
        log.info("Recalculating team statistics for season id: {}", seasonId);

        try {
            Season season = seasonService.getSeasonById(seasonId);
            List<Team> teams = teamRepository.findAll();

            int updated = 0;
            for (Team team : teams) {
                try {
                    statisticsCalculationService.calculateTeamSeasonStats(team, season);
                    updated++;
                } catch (Exception e) {
                    log.error("Error calculating stats for team id: {}", team.getId(), e);
                }
            }

            log.info("Successfully recalculated statistics for {} teams", updated);
        } catch (Exception e) {
            log.error("Error recalculating team season stats for season id: {}", seasonId, e);
        }
    }

    /**
     * Sync all data for a season (full refresh)
     */
    @Async
    public void fullSeasonDataSync(Long seasonId) {
        log.info("Starting full data sync for season id: {}", seasonId);

        recalculateTeamSeasonStats(seasonId);
        recalculatePlayerSeasonStats(seasonId);

        log.info("Completed full data sync for season id: {}", seasonId);
    }
}
