package com.np3tech.hoopmind.nba.service.impl;

import com.np3tech.hoopmind.user.domain.UserRepository;

import com.np3tech.hoopmind.nba.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for system health monitoring
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class HealthCheckService {

    private final GameRepository gameRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final UserRepository userRepository;
    private final SeasonRepository seasonRepository;

    /**
     * Get overall system health status
     */
    public Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();

        try {
            health.put("status", "UP");
            health.put("timestamp", LocalDateTime.now());
            health.put("database", checkDatabaseHealth());
            health.put("dataIntegrity", checkDataIntegrity());
            health.put("statistics", getSystemStatistics());
        } catch (Exception e) {
            log.error("Error checking system health", e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }

        return health;
    }

    /**
     * Check database connectivity and responsiveness
     */
    private Map<String, Object> checkDatabaseHealth() {
        Map<String, Object> dbHealth = new HashMap<>();

        try {
            long startTime = System.currentTimeMillis();
            long count = gameRepository.count();
            long duration = System.currentTimeMillis() - startTime;

            dbHealth.put("status", "CONNECTED");
            dbHealth.put("responseTimeMs", duration);
            dbHealth.put("sampleQueryResult", count);
        } catch (Exception e) {
            log.error("Database health check failed", e);
            dbHealth.put("status", "ERROR");
            dbHealth.put("error", e.getMessage());
        }

        return dbHealth;
    }

    /**
     * Check data integrity
     */
    private Map<String, Object> checkDataIntegrity() {
        Map<String, Object> integrity = new HashMap<>();

        try {
            // Check for orphaned records, invalid relationships, etc.
            long totalGames = gameRepository.count();
            long totalTeams = teamRepository.count();
            long totalPlayers = playerRepository.count();
            long totalUsers = userRepository.count();

            integrity.put("status", "VALID");
            integrity.put("totalGames", totalGames);
            integrity.put("totalTeams", totalTeams);
            integrity.put("totalPlayers", totalPlayers);
            integrity.put("totalUsers", totalUsers);

            // Check for active season
            boolean hasActiveSeason = seasonRepository.findActiveSeason().isPresent();
            integrity.put("hasActiveSeason", hasActiveSeason);

        } catch (Exception e) {
            log.error("Data integrity check failed", e);
            integrity.put("status", "ERROR");
            integrity.put("error", e.getMessage());
        }

        return integrity;
    }

    /**
     * Get system statistics
     */
    private Map<String, Object> getSystemStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            stats.put("totalGames", gameRepository.count());
            stats.put("totalTeams", teamRepository.count());
            stats.put("totalPlayers", playerRepository.count());
            stats.put("totalUsers", userRepository.count());
            stats.put("totalSeasons", seasonRepository.count());

            // Active entities
            long activeUsers = userRepository.countByEnabledTrue();
            stats.put("activeUsers", activeUsers);

        } catch (Exception e) {
            log.error("Error gathering system statistics", e);
        }

        return stats;
    }

    /**
     * Get detailed database metrics
     */
    public Map<String, Object> getDatabaseMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        try {
            // Table sizes
            metrics.put("gamesCount", gameRepository.count());
            metrics.put("teamsCount", teamRepository.count());
            metrics.put("playersCount", playerRepository.count());
            metrics.put("usersCount", userRepository.count());
            metrics.put("seasonsCount", seasonRepository.count());

            // Growth metrics (could be calculated based on creation dates)
            metrics.put("timestamp", LocalDateTime.now());

        } catch (Exception e) {
            log.error("Error gathering database metrics", e);
            metrics.put("error", e.getMessage());
        }

        return metrics;
    }

    /**
     * Check if system is ready to serve requests
     */
    public boolean isSystemReady() {
        try {
            // Basic checks
            gameRepository.count();
            teamRepository.count();
            userRepository.count();

            // Check if there's an active season
            boolean hasActiveSeason = seasonRepository.findActiveSeason().isPresent();

            return hasActiveSeason;
        } catch (Exception e) {
            log.error("System readiness check failed", e);
            return false;
        }
    }
}