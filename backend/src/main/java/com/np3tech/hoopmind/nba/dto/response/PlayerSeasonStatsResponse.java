package com.np3tech.hoopmind.nba.dto.response;

import com.np3tech.hoopmind.nba.entity.PlayerStats;

import java.math.BigDecimal;

public record PlayerSeasonStatsResponse(long playerId, SeasonSummaryResponse season, Integer gamesPlayed,
                                        Integer gamesStarted, BigDecimal minutesPerGame, BigDecimal pointsPerGame,
                                        BigDecimal totalReboundsPerGame, BigDecimal assistsPerGame,
                                        BigDecimal stealsPerGame, BigDecimal blocksPerGame, BigDecimal turnoversPerGame,
                                        BigDecimal fieldGoalPercentage, BigDecimal threePointPercentage,
                                        BigDecimal freeThrowPercentage) {

    public static PlayerSeasonStatsResponse of(PlayerStats stats) {
        return new PlayerSeasonStatsResponse(stats.getPlayer().getId(), SeasonSummaryResponse.of(stats.getSeason()),
                stats.getGamesPlayed(), stats.getGamesStarted(), stats.getMinutesPerGame(), stats.getPointsPerGame(),
                stats.getTotalReboundsPerGame(), stats.getAssistsPerGame(), stats.getStealsPerGame(),
                stats.getBlocksPerGame(), stats.getTurnoversPerGame(), stats.getFieldGoalPercentage(),
                stats.getThreePointPercentage(), stats.getFreeThrowPercentage());
    }
}
