package com.np3tech.hoopmind.nba.service;

import com.np3tech.hoopmind.nba.entity.PlayerGameStats;

import java.util.List;

public interface PlayerGameStatsService {
    PlayerGameStats createPlayerGameStats(PlayerGameStats stats);
    PlayerGameStats updatePlayerGameStats(Long id, PlayerGameStats statsUpdate);
    PlayerGameStats getPlayerGameStatsById(Long id);
    PlayerGameStats getPlayerGameStatsByPlayerAndGame(Long playerId, Long gameId);
    List<PlayerGameStats> getPlayerGameStatsByGame(Long gameId);
    List<PlayerGameStats> getPlayerGameStatsByPlayer(Long playerId);
    void deletePlayerGameStats(Long id);
}
