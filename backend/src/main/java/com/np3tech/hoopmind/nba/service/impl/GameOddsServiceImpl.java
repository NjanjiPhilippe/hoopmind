package com.np3tech.hoopmind.nba.service.impl;

import com.np3tech.hoopmind.nba.entity.Game;
import com.np3tech.hoopmind.nba.entity.GameOdds;
import com.np3tech.hoopmind.shared.exception.AppException;
import com.np3tech.hoopmind.nba.repository.GameOddsRepository;
import com.np3tech.hoopmind.nba.service.GameOddsService;
import com.np3tech.hoopmind.nba.service.GameService;
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
public class GameOddsServiceImpl implements GameOddsService {

    private final GameOddsRepository gameOddsRepository;
    private final GameService gameService;

    public GameOdds createGameOdds(GameOdds gameOdds) {
        log.info("Creating game odds for game id: {} from bookmaker: {}",
                gameOdds.getGame().getId(), gameOdds.getBookmaker());

        validateGameOdds(gameOdds);

        return gameOddsRepository.save(gameOdds);
    }

    public GameOdds updateGameOdds(Long id, GameOdds gameOddsUpdate) {
        log.info("Updating game odds with id: {}", id);

        GameOdds existingOdds = getGameOddsById(id);

        if (gameOddsUpdate.getHomeTeamMoneyline() != null) {
            existingOdds.setHomeTeamMoneyline(gameOddsUpdate.getHomeTeamMoneyline());
        }
        if (gameOddsUpdate.getAwayTeamMoneyline() != null) {
            existingOdds.setAwayTeamMoneyline(gameOddsUpdate.getAwayTeamMoneyline());
        }
        if (gameOddsUpdate.getSpread() != null) {
            existingOdds.setSpread(gameOddsUpdate.getSpread());
        }
        if (gameOddsUpdate.getOverUnder() != null) {
            existingOdds.setOverUnder(gameOddsUpdate.getOverUnder());
        }

        return gameOddsRepository.save(existingOdds);
    }

    @Transactional(readOnly = true)
    public GameOdds getGameOddsById(Long id) {
        return gameOddsRepository.findById(id)
                .orElseThrow(() -> new AppException(
                        "Game odds not found with id: " + id,
                        HttpStatus.NOT_FOUND,
                        "GAME_ODDS_NOT_FOUND"
                ));
    }

    @Transactional(readOnly = true)
    public List<GameOdds> getGameOddsByGameId(Long gameId) {
        Game game = gameService.getGameById(gameId);
        return gameOddsRepository.findByGame(game);
    }

    @Transactional(readOnly = true)
    public List<GameOdds> getLatestOddsByGameId(Long gameId) {
        Game game = gameService.getGameById(gameId);
        return gameOddsRepository.findLatestOddsByGame(game);
    }

    @Transactional(readOnly = true)
    public List<GameOdds> getGameOddsByBookmaker(String bookmaker) {
        return gameOddsRepository.findByBookmaker(bookmaker);
    }

    @Transactional(readOnly = true)
    public GameOdds getGameOddsByGameAndBookmaker(Long gameId, String bookmaker) {
        Game game = gameService.getGameById(gameId);
        return gameOddsRepository.findByGameAndBookmaker(game, bookmaker)
                .orElseThrow(() -> new AppException(
                        "Game odds not found for game id: " + gameId + " and bookmaker: " + bookmaker,
                        HttpStatus.NOT_FOUND,
                        "GAME_ODDS_NOT_FOUND"
                ));
    }

    public void deleteGameOdds(Long id) {
        log.info("Deleting game odds with id: {}", id);

        if (!gameOddsRepository.existsById(id)) {
            throw new AppException(
                    "Game odds not found with id: " + id,
                    HttpStatus.NOT_FOUND,
                    "GAME_ODDS_NOT_FOUND"
            );
        }

        gameOddsRepository.deleteById(id);
    }

    private void validateGameOdds(GameOdds gameOdds) {
        if (gameOdds.getGame() == null) {
            throw new AppException(
                    "Game is required for game odds",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_GAME_ODDS_DATA"
            );
        }

        if (gameOdds.getBookmaker() == null || gameOdds.getBookmaker().trim().isEmpty()) {
            throw new AppException(
                    "Bookmaker is required",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_GAME_ODDS_DATA"
            );
        }
    }
}
