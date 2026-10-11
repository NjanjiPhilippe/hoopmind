package com.np3tech.hoopmind.nba.service;

import com.np3tech.hoopmind.nba.entity.Game;
import com.np3tech.hoopmind.nba.entity.GameResult;
import com.np3tech.hoopmind.nba.entity.enummeration.GameStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface GameService {
    Game createGame(Game game);
    Game updateGame(Long id, Game gameUpdate);
    Game getGameById(Long id);
    List<Game> getAllGames();
    List<Game> getGamesByStatus(GameStatus status);
    List<Game> getGamesBySeason(Long seasonId);
    List<Game> getGamesByTeam(Long teamId);
    List<Game> getGamesByDateRange(LocalDateTime start, LocalDateTime end);
    List<Game> getGamesBySeasonAndTeam(Long seasonId, Long teamId);
    void updateGameStatus(Long id, GameStatus status);
    void deleteGame(Long id);

    /** Résultats des matchs déjà joués, indexés par identifiant de match (en une requête). */
    Map<Long, GameResult> getResults(Collection<Game> games);
}
