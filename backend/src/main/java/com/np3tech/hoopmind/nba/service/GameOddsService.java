package com.np3tech.hoopmind.nba.service;

import com.np3tech.hoopmind.nba.entity.GameOdds;

import java.util.List;

public interface GameOddsService {
    GameOdds createGameOdds(GameOdds gameOdds);
    GameOdds updateGameOdds(Long id, GameOdds gameOddsUpdate);
    GameOdds getGameOddsById(Long id);
    List<GameOdds> getGameOddsByGameId(Long gameId);
    List<GameOdds> getLatestOddsByGameId(Long gameId);
    List<GameOdds> getGameOddsByBookmaker(String bookmaker);
    GameOdds getGameOddsByGameAndBookmaker(Long gameId, String bookmaker);
    void deleteGameOdds(Long id);
}
