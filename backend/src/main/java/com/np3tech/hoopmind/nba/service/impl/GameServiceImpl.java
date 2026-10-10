package com.np3tech.hoopmind.nba.service.impl;

import com.np3tech.hoopmind.nba.entity.Game;
import com.np3tech.hoopmind.nba.entity.Season;
import com.np3tech.hoopmind.nba.entity.Team;
import com.np3tech.hoopmind.nba.entity.enummeration.GameStatus;
import com.np3tech.hoopmind.shared.exception.AppException;
import com.np3tech.hoopmind.nba.repository.GameRepository;
import com.np3tech.hoopmind.nba.service.GameService;
import com.np3tech.hoopmind.nba.service.SeasonService;
import com.np3tech.hoopmind.nba.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GameServiceImpl implements GameService {

    private final GameRepository gameRepository;
    private final TeamService teamService;
    private final SeasonService seasonService;

    public Game createGame(Game game) {
        log.info("Creating game: {} vs {}", game.getHomeTeam().getName(), game.getAwayTeam().getName());

        validateGame(game);

        return gameRepository.save(game);
    }

    public Game updateGame(Long id, Game gameUpdate) {
        log.info("Updating game with id: {}", id);

        Game existingGame = getGameById(id);

        if (gameUpdate.getScheduledDate() != null) {
            existingGame.setScheduledDate(gameUpdate.getScheduledDate());
        }
        if (gameUpdate.getStatus() != null) {
            existingGame.setStatus(gameUpdate.getStatus());
        }
        if (gameUpdate.getActualStartTime() != null) {
            existingGame.setActualStartTime(gameUpdate.getActualStartTime());
        }
        if (gameUpdate.getActualEndTime() != null) {
            existingGame.setActualEndTime(gameUpdate.getActualEndTime());
        }
        if (gameUpdate.getVenue() != null) {
            existingGame.setVenue(gameUpdate.getVenue());
        }
        if (gameUpdate.getAttendance() != null) {
            existingGame.setAttendance(gameUpdate.getAttendance());
        }

        return gameRepository.save(existingGame);
    }

    @Transactional(readOnly = true)
    public Game getGameById(Long id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new AppException(
                        "Game not found with id: " + id,
                        HttpStatus.NOT_FOUND,
                        "GAME_NOT_FOUND"
                ));
    }

    @Transactional(readOnly = true)
    public List<Game> getAllGames() {
        return gameRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Game> getGamesByStatus(GameStatus status) {
        return gameRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<Game> getGamesBySeason(Long seasonId) {
        Season season = seasonService.getSeasonById(seasonId);
        return gameRepository.findBySeason(season);
    }

    @Transactional(readOnly = true)
    public List<Game> getGamesByTeam(Long teamId) {
        Team team = teamService.getTeamById(teamId);
        return gameRepository.findByHomeTeamOrAwayTeam(team, team);
    }

    @Transactional(readOnly = true)
    public List<Game> getGamesByDateRange(LocalDateTime start, LocalDateTime end) {
        return gameRepository.findByScheduledDateBetween(start, end);
    }

    @Transactional(readOnly = true)
    public List<Game> getGamesBySeasonAndTeam(Long seasonId, Long teamId) {
        Season season = seasonService.getSeasonById(seasonId);
        Team team = teamService.getTeamById(teamId);
        return gameRepository.findBySeasonAndTeam(season, team);
    }

    public void updateGameStatus(Long id, GameStatus status) {
        log.info("Updating game status for id: {} to {}", id, status);

        Game game = getGameById(id);
        game.setStatus(status);

        if (status == GameStatus.IN_PROGRESS && game.getActualStartTime() == null) {
            game.setActualStartTime(LocalDateTime.now());
        } else if (status == GameStatus.COMPLETED && game.getActualEndTime() == null) {
            game.setActualEndTime(LocalDateTime.now());
        }

        gameRepository.save(game);
    }

    public void deleteGame(Long id) {
        log.info("Deleting game with id: {}", id);

        if (!gameRepository.existsById(id)) {
            throw new AppException(
                    "Game not found with id: " + id,
                    HttpStatus.NOT_FOUND,
                    "GAME_NOT_FOUND"
            );
        }

        gameRepository.deleteById(id);
    }

    private void validateGame(Game game) {
        if (game.getHomeTeam() == null || game.getAwayTeam() == null) {
            throw new AppException(
                    "Home team and away team are required",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_GAME_DATA"
            );
        }

        if (game.getHomeTeam().getId().equals(game.getAwayTeam().getId())) {
            throw new AppException(
                    "Home team and away team cannot be the same",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_GAME_DATA"
            );
        }

        if (game.getSeason() == null) {
            throw new AppException(
                    "Season is required",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_GAME_DATA"
            );
        }
    }
}
