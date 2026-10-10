package com.np3tech.hoopmind.nba.service;

import com.np3tech.hoopmind.nba.entity.GameResult;

import java.util.List;

public interface GameResultService {
    GameResult createGameResult(GameResult gameResult);
    GameResult updateGameResult(Long id, GameResult gameResultUpdate);
    GameResult getGameResultById(Long id);
    GameResult getGameResultByGameId(Long gameId);
    List<GameResult> getAllGameResults();
    List<GameResult> getGameResultsByTeam(Long teamId);
    List<GameResult> getOvertimeGames();
    void deleteGameResult(Long id);
}
