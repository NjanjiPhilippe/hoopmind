package com.np3tech.hoopmind.nba.service.impl;

import com.np3tech.hoopmind.nba.entity.Game;
import com.np3tech.hoopmind.nba.entity.Team;
import com.np3tech.hoopmind.nba.entity.TeamGameStats;
import com.np3tech.hoopmind.shared.exception.AppException;
import com.np3tech.hoopmind.nba.repository.TeamGameStatsRepository;
import com.np3tech.hoopmind.nba.service.GameService;
import com.np3tech.hoopmind.nba.service.TeamGameStatsService;
import com.np3tech.hoopmind.nba.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TeamGameStatsServiceImpl implements TeamGameStatsService {

    private final TeamGameStatsRepository teamGameStatsRepository;
    private final TeamService teamService;
    private final GameService gameService;

    public TeamGameStats createTeamGameStats(TeamGameStats stats) {
        log.info("Creating team game stats for team id: {} and game id: {}",
                stats.getTeam().getId(), stats.getGame().getId());

        validateTeamGameStats(stats);

        return teamGameStatsRepository.save(stats);
    }

    public TeamGameStats updateTeamGameStats(Long id, TeamGameStats statsUpdate) {
        log.info("Updating team game stats with id: {}", id);

        TeamGameStats existing = getTeamGameStatsById(id);

        if (statsUpdate.getPoints() != null) existing.setPoints(statsUpdate.getPoints());
        if (statsUpdate.getFieldGoalsMade() != null) existing.setFieldGoalsMade(statsUpdate.getFieldGoalsMade());
        if (statsUpdate.getTotalRebounds() != null) existing.setTotalRebounds(statsUpdate.getTotalRebounds());
        if (statsUpdate.getAssists() != null) existing.setAssists(statsUpdate.getAssists());

        return teamGameStatsRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public TeamGameStats getTeamGameStatsById(Long id) {
        return teamGameStatsRepository.findById(id)
                .orElseThrow(() -> new AppException(
                        "Team game stats not found with id: " + id,
                        HttpStatus.NOT_FOUND,
                        "TEAM_GAME_STATS_NOT_FOUND"
                ));
    }

    @Transactional(readOnly = true)
    public List<TeamGameStats> getTeamGameStatsByGame(Long gameId) {
        Game game = gameService.getGameById(gameId);
        return teamGameStatsRepository.findByGame(game);
    }

    @Transactional(readOnly = true)
    public List<TeamGameStats> getTeamGameStatsByTeam(Long teamId) {
        Team team = teamService.getTeamById(teamId);
        return teamGameStatsRepository.findByTeamOrderByDateDesc(team);
    }

    public void deleteTeamGameStats(Long id) {
        log.info("Deleting team game stats with id: {}", id);

        if (!teamGameStatsRepository.existsById(id)) {
            throw new AppException(
                    "Team game stats not found with id: " + id,
                    HttpStatus.NOT_FOUND,
                    "TEAM_GAME_STATS_NOT_FOUND"
            );
        }

        teamGameStatsRepository.deleteById(id);
    }

    private void validateTeamGameStats(TeamGameStats stats) {
        if (stats.getTeam() == null) {
            throw new AppException(
                    "Team is required for team game stats",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_TEAM_GAME_STATS_DATA"
            );
        }

        if (stats.getGame() == null) {
            throw new AppException(
                    "Game is required for team game stats",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_TEAM_GAME_STATS_DATA"
            );
        }
    }
}
