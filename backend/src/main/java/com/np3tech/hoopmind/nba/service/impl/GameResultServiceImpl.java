package com.np3tech.hoopmind.nba.service.impl;

import com.np3tech.hoopmind.nba.entity.Game;
import com.np3tech.hoopmind.nba.entity.GameResult;
import com.np3tech.hoopmind.nba.entity.enummeration.GameStatus;
import com.np3tech.hoopmind.shared.exception.AppException;
import com.np3tech.hoopmind.nba.repository.GameResultRepository;
import com.np3tech.hoopmind.nba.service.GameResultService;
import com.np3tech.hoopmind.nba.service.GameService;
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
public class GameResultServiceImpl implements GameResultService {

    private final GameResultRepository gameResultRepository;
    private final GameService gameService;

    public GameResult createGameResult(GameResult gameResult) {
        log.info("Creating game result for game id: {}", gameResult.getGame().getId());

        validateGameResult(gameResult);

        // Check if result already exists for this game
        gameResultRepository.findByGame(gameResult.getGame())
                .ifPresent(existing -> {
                    throw new AppException(
                            "Game result already exists for game id: " + gameResult.getGame().getId(),
                            HttpStatus.CONFLICT,
                            "GAME_RESULT_ALREADY_EXISTS"
                    );
                });

        // Calculate score difference and determine winner/loser
        int homScore = gameResult.getHomeTeamScore();
        int awayScore = gameResult.getAwayTeamScore();
        int scoreDiff = Math.abs(homScore - awayScore);

        gameResult.setScoreDifference(scoreDiff);

        if (homScore > awayScore) {
            gameResult.setWinner(gameResult.getGame().getHomeTeam());
            gameResult.setLoser(gameResult.getGame().getAwayTeam());
        } else {
            gameResult.setWinner(gameResult.getGame().getAwayTeam());
            gameResult.setLoser(gameResult.getGame().getHomeTeam());
        }

        gameResult.setRecordedAt(LocalDateTime.now());

        // Update game status to FINISHED
        gameService.updateGameStatus(gameResult.getGame().getId(), GameStatus.COMPLETED);

        return gameResultRepository.save(gameResult);
    }

    public GameResult updateGameResult(Long id, GameResult gameResultUpdate) {
        log.info("Updating game result with id: {}", id);

        GameResult existingResult = getGameResultById(id);

        if (gameResultUpdate.getHomeTeamScore() != null) {
            existingResult.setHomeTeamScore(gameResultUpdate.getHomeTeamScore());
        }
        if (gameResultUpdate.getAwayTeamScore() != null) {
            existingResult.setAwayTeamScore(gameResultUpdate.getAwayTeamScore());
        }

        // Recalculate winner/loser and score difference
        int homeScore = existingResult.getHomeTeamScore();
        int awayScore = existingResult.getAwayTeamScore();
        int scoreDiff = Math.abs(homeScore - awayScore);

        existingResult.setScoreDifference(scoreDiff);

        if (homeScore > awayScore) {
            existingResult.setWinner(existingResult.getGame().getHomeTeam());
            existingResult.setLoser(existingResult.getGame().getAwayTeam());
        } else {
            existingResult.setWinner(existingResult.getGame().getAwayTeam());
            existingResult.setLoser(existingResult.getGame().getHomeTeam());
        }

        if (gameResultUpdate.getOvertime() != null) {
            existingResult.setOvertime(gameResultUpdate.getOvertime());
        }
        if (gameResultUpdate.getOvertimePeriods() != null) {
            existingResult.setOvertimePeriods(gameResultUpdate.getOvertimePeriods());
        }

        return gameResultRepository.save(existingResult);
    }

    @Transactional(readOnly = true)
    public GameResult getGameResultById(Long id) {
        return gameResultRepository.findById(id)
                .orElseThrow(() -> new AppException(
                        "Game result not found with id: " + id,
                        HttpStatus.NOT_FOUND,
                        "GAME_RESULT_NOT_FOUND"
                ));
    }

    @Transactional(readOnly = true)
    public GameResult getGameResultByGameId(Long gameId) {
        Game game = gameService.getGameById(gameId);
        return gameResultRepository.findByGame(game)
                .orElseThrow(() -> new AppException(
                        "Game result not found for game id: " + gameId,
                        HttpStatus.NOT_FOUND,
                        "GAME_RESULT_NOT_FOUND"
                ));
    }

    @Transactional(readOnly = true)
    public List<GameResult> getAllGameResults() {
        return gameResultRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<GameResult> getGameResultsByTeam(Long teamId) {
        return gameResultRepository.findByTeam(null); // Will be injected by repository
    }

    @Transactional(readOnly = true)
    public List<GameResult> getOvertimeGames() {
        return gameResultRepository.findByOvertimeTrue();
    }

    public void deleteGameResult(Long id) {
        log.info("Deleting game result with id: {}", id);

        if (!gameResultRepository.existsById(id)) {
            throw new AppException(
                    "Game result not found with id: " + id,
                    HttpStatus.NOT_FOUND,
                    "GAME_RESULT_NOT_FOUND"
            );
        }

        gameResultRepository.deleteById(id);
    }

    private void validateGameResult(GameResult gameResult) {
        if (gameResult.getGame() == null) {
            throw new AppException(
                    "Game is required for game result",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_GAME_RESULT_DATA"
            );
        }

        if (gameResult.getHomeTeamScore() == null || gameResult.getAwayTeamScore() == null) {
            throw new AppException(
                    "Home team score and away team score are required",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_GAME_RESULT_DATA"
            );
        }

        if (gameResult.getHomeTeamScore().equals(gameResult.getAwayTeamScore())) {
            throw new AppException(
                    "Game cannot end in a tie",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_GAME_RESULT_DATA"
            );
        }
    }
}
