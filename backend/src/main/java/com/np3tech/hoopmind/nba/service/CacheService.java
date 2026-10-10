package com.np3tech.hoopmind.nba.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Service for managing application cache
 * Configure caching in application.yml for optimal performance
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    /**
     * Clear all caches
     */
    @CacheEvict(value = {"teams", "players", "games", "stats", "standings"}, allEntries = true)
    public void clearAllCaches() {
        log.info("All caches cleared");
    }

    /**
     * Clear team-related caches
     */
    @CacheEvict(value = {"teams", "standings"}, allEntries = true)
    public void clearTeamCaches() {
        log.info("Team caches cleared");
    }

    /**
     * Clear player-related caches
     */
    @CacheEvict(value = {"players", "stats"}, allEntries = true)
    public void clearPlayerCaches() {
        log.info("Player caches cleared");
    }

    /**
     * Clear game-related caches
     */
    @CacheEvict(value = {"games"}, allEntries = true)
    public void clearGameCaches() {
        log.info("Game caches cleared");
    }

    /**
     * Clear statistics caches
     */
    @CacheEvict(value = {"stats", "standings"}, allEntries = true)
    public void clearStatsCaches() {
        log.info("Statistics caches cleared");
    }

    /**
     * Clear cache for specific team
     */
    @CacheEvict(value = "teams", key = "#teamId")
    public void clearTeamCache(Long teamId) {
        log.info("Cache cleared for team id: {}", teamId);
    }

    /**
     * Clear cache for specific player
     */
    @CacheEvict(value = "players", key = "#playerId")
    public void clearPlayerCache(Long playerId) {
        log.info("Cache cleared for player id: {}", playerId);
    }

    /**
     * Clear cache for specific game
     */
    @CacheEvict(value = "games", key = "#gameId")
    public void clearGameCache(Long gameId) {
        log.info("Cache cleared for game id: {}", gameId);
    }
}
