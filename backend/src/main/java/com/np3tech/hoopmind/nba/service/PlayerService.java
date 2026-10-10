package com.np3tech.hoopmind.nba.service;

import com.np3tech.hoopmind.nba.entity.Player;
import com.np3tech.hoopmind.nba.entity.enummeration.PlayerStatus;
import com.np3tech.hoopmind.nba.entity.enummeration.Position;

import java.util.List;

public interface PlayerService {
    Player createPlayer(Player player);
    Player updatePlayer(Long id, Player playerUpdate);
    Player getPlayerById(Long id);
    List<Player> getAllPlayers();
    List<Player> getPlayersByTeam(Long teamId);
    List<Player> getPlayersByPosition(Position position);
    List<Player> getPlayersByStatus(PlayerStatus status);
    List<Player> searchPlayersByName(String name);
    void updatePlayerStatus(Long id, PlayerStatus status);
    void deletePlayer(Long id);
}
