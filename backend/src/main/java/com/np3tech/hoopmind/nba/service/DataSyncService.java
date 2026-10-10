package com.np3tech.hoopmind.nba.service;

public interface DataSyncService {
    void updateGameStatistics(Long gameId);
    void evaluateGamePredictions(Long gameId);
    void recalculatePlayerSeasonStats(Long seasonId);
    void recalculateTeamSeasonStats(Long seasonId);
    void fullSeasonDataSync(Long seasonId);
}
