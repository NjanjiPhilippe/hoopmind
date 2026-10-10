package com.np3tech.hoopmind.nba.service.impl;

import com.np3tech.hoopmind.nba.entity.*;
import com.np3tech.hoopmind.nba.repository.*;
import com.np3tech.hoopmind.nba.service.StatisticsCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StatisticsCalculationServiceImpl implements StatisticsCalculationService {

    private final PlayerGameStatsRepository playerGameStatsRepository;
    private final TeamGameStatsRepository teamGameStatsRepository;
    private final PlayerStatsRepository playerStatsRepository;
    private final TeamStatsRepository teamStatsRepository;
    private final GameResultRepository gameResultRepository;

    /**
     * Calculate and update season statistics for a player
     */
    public PlayerStats calculatePlayerSeasonStats(Player player, Season season) {
        log.info("Calculating season stats for player: {} {}", player.getFirstName(), player.getLastName());

        List<PlayerGameStats> gameStats = playerGameStatsRepository.findByPlayer(player);

        if (gameStats.isEmpty()) {
            log.warn("No game stats found for player id: {}", player.getId());
            return null;
        }

        int gamesPlayed = gameStats.size();
        int gamesStarted = (int) gameStats.stream().filter(PlayerGameStats::getStarted).count();

        // Calculate averages
        double totalMinutes = gameStats.stream().mapToInt(s -> s.getMinutesPlayed() != null ? s.getMinutesPlayed() : 0).sum();
        double totalPoints = gameStats.stream().mapToInt(s -> s.getPoints() != null ? s.getPoints() : 0).sum();
        double totalRebounds = gameStats.stream().mapToInt(s -> s.getTotalRebounds() != null ? s.getTotalRebounds() : 0).sum();
        double totalAssists = gameStats.stream().mapToInt(s -> s.getAssists() != null ? s.getAssists() : 0).sum();
        double totalSteals = gameStats.stream().mapToInt(s -> s.getSteals() != null ? s.getSteals() : 0).sum();
        double totalBlocks = gameStats.stream().mapToInt(s -> s.getBlocks() != null ? s.getBlocks() : 0).sum();
        double totalTurnovers = gameStats.stream().mapToInt(s -> s.getTurnovers() != null ? s.getTurnovers() : 0).sum();

        // Calculate shooting percentages
        int totalFGM = gameStats.stream().mapToInt(s -> s.getFieldGoalsMade() != null ? s.getFieldGoalsMade() : 0).sum();
        int totalFGA = gameStats.stream().mapToInt(s -> s.getFieldGoalsAttempted() != null ? s.getFieldGoalsAttempted() : 0).sum();
        int total3PM = gameStats.stream().mapToInt(s -> s.getThreePointsMade() != null ? s.getThreePointsMade() : 0).sum();
        int total3PA = gameStats.stream().mapToInt(s -> s.getThreePointsAttempted() != null ? s.getThreePointsAttempted() : 0).sum();
        int totalFTM = gameStats.stream().mapToInt(s -> s.getFreeThrowsMade() != null ? s.getFreeThrowsMade() : 0).sum();
        int totalFTA = gameStats.stream().mapToInt(s -> s.getFreeThrowsAttempted() != null ? s.getFreeThrowsAttempted() : 0).sum();

        PlayerStats stats = playerStatsRepository.findByPlayerAndSeason(player, season)
                .orElse(PlayerStats.builder()
                        .player(player)
                        .season(season)
                        .build());

        stats.setGamesPlayed(gamesPlayed);
        stats.setGamesStarted(gamesStarted);
        stats.setMinutesPerGame(divide(totalMinutes, gamesPlayed));
        stats.setPointsPerGame(divide(totalPoints, gamesPlayed));
        stats.setTotalReboundsPerGame(divide(totalRebounds, gamesPlayed));
        stats.setAssistsPerGame(divide(totalAssists, gamesPlayed));
        stats.setStealsPerGame(divide(totalSteals, gamesPlayed));
        stats.setBlocksPerGame(divide(totalBlocks, gamesPlayed));
        stats.setTurnoversPerGame(divide(totalTurnovers, gamesPlayed));

        stats.setFieldGoalsMade(divide(totalFGM, gamesPlayed));
        stats.setFieldGoalsAttempted(divide(totalFGA, gamesPlayed));
        stats.setFieldGoalPercentage(calculatePercentage(totalFGM, totalFGA));

        stats.setThreePointsMade(divide(total3PM, gamesPlayed));
        stats.setThreePointsAttempted(divide(total3PA, gamesPlayed));
        stats.setThreePointPercentage(calculatePercentage(total3PM, total3PA));

        stats.setFreeThrowsMade(divide(totalFTM, gamesPlayed));
        stats.setFreeThrowsAttempted(divide(totalFTA, gamesPlayed));
        stats.setFreeThrowPercentage(calculatePercentage(totalFTM, totalFTA));

        // Calculate advanced stats
        stats.setTrueShootingPercentage(calculateTrueShootingPercentage(totalPoints, totalFGA, totalFTA));
        stats.setEffectiveFieldGoalPercentage(calculateEffectiveFGPercentage(totalFGM, total3PM, totalFGA));

        stats.setLastUpdated(LocalDateTime.now());

        return playerStatsRepository.save(stats);
    }

    /**
     * Calculate and update season statistics for a team
     */
    public TeamStats calculateTeamSeasonStats(Team team, Season season) {
        log.info("Calculating season stats for team: {}", team.getName());

        // Get all games for the team
        List<GameResult> results = gameResultRepository.findByTeam(team);

        if (results.isEmpty()) {
            log.warn("No game results found for team id: {}", team.getId());
            return null;
        }

        int wins = 0;
        int losses = 0;
        int homeWins = 0;
        int homeLosses = 0;
        int awayWins = 0;
        int awayLosses = 0;

        double totalPointsFor = 0;
        double totalPointsAgainst = 0;

        for (GameResult result : results) {
            boolean isWinner = result.getWinner().getId().equals(team.getId());
            boolean isHomeTeam = result.getGame().getHomeTeam().getId().equals(team.getId());

            if (isWinner) {
                wins++;
                if (isHomeTeam) homeWins++;
                else awayWins++;
            } else {
                losses++;
                if (isHomeTeam) homeLosses++;
                else awayLosses++;
            }

            if (isHomeTeam) {
                totalPointsFor += result.getHomeTeamScore();
                totalPointsAgainst += result.getAwayTeamScore();
            } else {
                totalPointsFor += result.getAwayTeamScore();
                totalPointsAgainst += result.getHomeTeamScore();
            }
        }

        int totalGames = wins + losses;

        TeamStats stats = teamStatsRepository.findByTeamAndSeason(team, season)
                .orElse(TeamStats.builder()
                        .team(team)
                        .season(season)
                        .build());

        stats.setWins(wins);
        stats.setLosses(losses);
        stats.setWinPercentage(calculatePercentage(wins, totalGames));
        stats.setHomeWins(homeWins);
        stats.setHomeLosses(homeLosses);
        stats.setAwayWins(awayWins);
        stats.setAwayLosses(awayLosses);

        stats.setPointsPerGame(divide(totalPointsFor, totalGames));
        stats.setPointsAllowedPerGame(divide(totalPointsAgainst, totalGames));
        stats.setPointsDifferential(divide(totalPointsFor - totalPointsAgainst, totalGames));

        stats.setLastUpdated(LocalDateTime.now());

        return teamStatsRepository.save(stats);
    }

    /**
     * Calculate win probability based on team stats
     */
    public BigDecimal calculateWinProbability(Team homeTeam, Team awayTeam, Season season) {
        TeamStats homeStats = teamStatsRepository.findByTeamAndSeason(homeTeam, season).orElse(null);
        TeamStats awayStats = teamStatsRepository.findByTeamAndSeason(awayTeam, season).orElse(null);

        if (homeStats == null || awayStats == null) {
            return BigDecimal.valueOf(0.5); // 50-50 if no stats available
        }

        // Simple probability calculation based on win percentage and home court advantage
        double homeWinPct = homeStats.getWinPercentage() != null ? homeStats.getWinPercentage().doubleValue() : 0.5;
        double awayWinPct = awayStats.getWinPercentage() != null ? awayStats.getWinPercentage().doubleValue() : 0.5;

        // Add home court advantage (typically 5-10%)
        double homeAdvantage = 0.07;
        double adjustedHomeWinPct = homeWinPct + homeAdvantage;

        // Calculate probability
        double totalStrength = adjustedHomeWinPct + awayWinPct;
        double probability = totalStrength > 0 ? adjustedHomeWinPct / totalStrength : 0.5;

        return BigDecimal.valueOf(probability).setScale(4, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Calculate Player Efficiency Rating (PER)
     */
    public BigDecimal calculatePER(PlayerGameStats stats) {
        if (stats.getMinutesPlayed() == null || stats.getMinutesPlayed() == 0) {
            return BigDecimal.ZERO;
        }

        double points = stats.getPoints() != null ? stats.getPoints() : 0;
        double rebounds = stats.getTotalRebounds() != null ? stats.getTotalRebounds() : 0;
        double assists = stats.getAssists() != null ? stats.getAssists() : 0;
        double steals = stats.getSteals() != null ? stats.getSteals() : 0;
        double blocks = stats.getBlocks() != null ? stats.getBlocks() : 0;
        double turnovers = stats.getTurnovers() != null ? stats.getTurnovers() : 0;
        double fgm = stats.getFieldGoalsMade() != null ? stats.getFieldGoalsMade() : 0;
        double fga = stats.getFieldGoalsAttempted() != null ? stats.getFieldGoalsAttempted() : 0;
        double ftm = stats.getFreeThrowsMade() != null ? stats.getFreeThrowsMade() : 0;
        double fta = stats.getFreeThrowsAttempted() != null ? stats.getFreeThrowsAttempted() : 0;

        // Simplified PER formula
        double per = (points + rebounds + assists + steals + blocks - (fga - fgm) - (fta - ftm) - turnovers) / stats.getMinutesPlayed();

        return BigDecimal.valueOf(per * 10).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    // Helper methods
    private BigDecimal divide(double numerator, double denominator) {
        if (denominator == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(numerator / denominator).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private BigDecimal calculatePercentage(int made, int attempted) {
        if (attempted == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf((double) made / attempted).setScale(3,BigDecimal.ROUND_HALF_UP);
    }

    private BigDecimal calculateTrueShootingPercentage(double points, double fga, double fta) {
        double possessions = fga + (0.44 * fta);
        if (possessions == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(points / (2 * possessions)).setScale(3,BigDecimal.ROUND_HALF_UP);
    }

    private BigDecimal calculateEffectiveFGPercentage(int fgm, int threepm, int fga) {
        if (fga == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf((fgm + 0.5 * threepm) / fga).setScale(3,BigDecimal.ROUND_HALF_UP);
    }
}
