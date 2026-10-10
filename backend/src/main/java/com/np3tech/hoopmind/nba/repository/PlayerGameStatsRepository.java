package com.np3tech.hoopmind.nba.repository;

import com.np3tech.hoopmind.nba.entity.Game;
import com.np3tech.hoopmind.nba.entity.Player;
import com.np3tech.hoopmind.nba.entity.PlayerGameStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerGameStatsRepository extends JpaRepository<PlayerGameStats, Long> {

    Optional<PlayerGameStats> findByPlayerAndGame(Player player, Game game);

    List<PlayerGameStats> findByGame(Game game);

    List<PlayerGameStats> findByPlayer(Player player);

    @Query("SELECT pgs FROM PlayerGameStats pgs WHERE pgs.player = :player ORDER BY pgs.game.scheduledDate DESC")
    List<PlayerGameStats> findByPlayerOrderByDateDesc(@Param("player") Player player);

    @Query("SELECT pgs FROM PlayerGameStats pgs WHERE pgs.game = :game AND pgs.points >= :minPoints ORDER BY pgs.points DESC")
    List<PlayerGameStats> findTopPerformersInGame(@Param("game") Game game, @Param("minPoints") Integer minPoints);

    void deleteByGame(Game game);
}