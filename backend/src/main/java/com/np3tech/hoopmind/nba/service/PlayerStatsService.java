package com.np3tech.hoopmind.nba.service;

import com.np3tech.hoopmind.nba.dto.request.PlayerSeasonStatsRequest;
import com.np3tech.hoopmind.nba.entity.Player;
import com.np3tech.hoopmind.nba.entity.PlayerStats;
import com.np3tech.hoopmind.nba.entity.Season;
import com.np3tech.hoopmind.nba.repository.PlayerStatsRepository;
import com.np3tech.hoopmind.shared.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** Statistiques de saison des joueurs, saisies par un administrateur (une ligne par joueur et par saison). */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PlayerStatsService {

    private final PlayerStatsRepository repository;
    private final PlayerService playerService;
    private final SeasonService seasonService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<PlayerStats> getPlayerStats(Long playerId) {
        return repository.findByPlayer(playerService.getPlayerById(playerId));
    }

    /** Crée ou remplace les statistiques de la saison : ce qui n'est pas envoyé est effacé. */
    public PlayerStats saveSeasonStats(Long playerId, Long seasonId, PlayerSeasonStatsRequest request) {
        if (request.gamesStarted() != null && request.gamesStarted() > request.gamesPlayed()) {
            throw new AppException("Un joueur ne peut pas être titulaire plus souvent qu'il n'a joué.",
                    HttpStatus.BAD_REQUEST, "INVALID_PLAYER_STATS");
        }
        Player player = playerService.getPlayerById(playerId);
        Season season = seasonService.getSeasonById(seasonId);
        PlayerStats stats = repository.findByPlayerAndSeason(player, season)
                .orElseGet(() -> PlayerStats.builder().player(player).season(season).build());
        stats.setGamesPlayed(request.gamesPlayed());
        stats.setGamesStarted(request.gamesStarted());
        stats.setMinutesPerGame(request.minutesPerGame());
        stats.setPointsPerGame(request.pointsPerGame());
        stats.setTotalReboundsPerGame(request.totalReboundsPerGame());
        stats.setAssistsPerGame(request.assistsPerGame());
        stats.setStealsPerGame(request.stealsPerGame());
        stats.setBlocksPerGame(request.blocksPerGame());
        stats.setTurnoversPerGame(request.turnoversPerGame());
        stats.setFieldGoalPercentage(request.fieldGoalPercentage());
        stats.setThreePointPercentage(request.threePointPercentage());
        stats.setFreeThrowPercentage(request.freeThrowPercentage());
        stats.setLastUpdated(LocalDateTime.now(clock));
        log.info("Statistiques saisies pour le joueur {} en {}", playerId, season.getName());
        return repository.save(stats);
    }

    public void deleteSeasonStats(Long playerId, Long seasonId) {
        Player player = playerService.getPlayerById(playerId);
        repository.delete(repository.findByPlayerAndSeason(player, seasonService.getSeasonById(seasonId))
                .orElseThrow(() -> AppException.notFound("PLAYER_STATS_NOT_FOUND", "Aucune statistique pour ce joueur et cette saison.")));
    }
}
