package com.np3tech.hoopmind.nba.mapper;

import com.np3tech.hoopmind.nba.dto.response.PlayerStatsResponse;
import com.np3tech.hoopmind.nba.dto.response.PlayerSummaryResponse;
import com.np3tech.hoopmind.nba.dto.response.SeasonSummaryResponse;
import com.np3tech.hoopmind.nba.entity.Player;
import com.np3tech.hoopmind.nba.entity.PlayerStats;
import com.np3tech.hoopmind.nba.entity.Season;
import org.springframework.stereotype.Component;

@Component
public class PlayerStatsMapper {

    public PlayerStatsResponse toResponse(PlayerStats stats) {
        return PlayerStatsResponse.builder()
                .id(stats.getId())
                .player(toPlayerSummary(stats.getPlayer()))
                .season(toSeasonSummary(stats.getSeason()))
                .gamesPlayed(stats.getGamesPlayed())
                .gamesStarted(stats.getGamesStarted())
                .minutesPerGame(stats.getMinutesPerGame())
                .pointsPerGame(stats.getPointsPerGame())
                .totalReboundsPerGame(stats.getTotalReboundsPerGame())
                .assistsPerGame(stats.getAssistsPerGame())
                .stealsPerGame(stats.getStealsPerGame())
                .blocksPerGame(stats.getBlocksPerGame())
                .turnoversPerGame(stats.getTurnoversPerGame())
                .fieldGoalPercentage(stats.getFieldGoalPercentage())
                .threePointPercentage(stats.getThreePointPercentage())
                .freeThrowPercentage(stats.getFreeThrowPercentage())
                .trueShootingPercentage(stats.getTrueShootingPercentage())
                .effectiveFieldGoalPercentage(stats.getEffectiveFieldGoalPercentage())
                .build();
    }

    private PlayerSummaryResponse toPlayerSummary(Player player) {
        return PlayerSummaryResponse.builder()
                .id(player.getId())
                .firstName(player.getFirstName())
                .lastName(player.getLastName())
                .position(player.getPosition())
                .jerseyNumber(player.getJerseyNumber())
                .photoUrl(player.getPhotoUrl())
                .build();
    }

    private SeasonSummaryResponse toSeasonSummary(Season season) {
        return SeasonSummaryResponse.builder()
                .id(season.getId())
                .year(season.getYear())
                .name(season.getName())
                .build();
    }
}

