package com.np3tech.hoopmind.nba.service;

import com.np3tech.hoopmind.nba.entity.PlayerStats;

import java.util.List;

public interface PlayerStatsService {
    PlayerStats createOrUpdatePlayerStats(PlayerStats playerStats);
    PlayerStats getPlayerStatsById(Long id);
    PlayerStats getPlayerStatsByPlayerAndSeason(Long playerId, Long seasonId);
    List<PlayerStats> getPlayerStatsBySeason(Long seasonId);
    List<PlayerStats> getTopScorers(Long seasonId);
    List<PlayerStats> getTopAssists(Long seasonId);
    List<PlayerStats> getTopRebounders(Long seasonId);
    void deletePlayerStats(Long id);
}
