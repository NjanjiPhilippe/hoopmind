package com.np3tech.hoopmind.nba.repository;

import com.np3tech.hoopmind.nba.entity.Player;
import com.np3tech.hoopmind.nba.entity.PlayerStats;
import com.np3tech.hoopmind.nba.entity.Season;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/** Les lectures chargent la saison : la réponse est construite hors session JPA. */
public interface PlayerStatsRepository extends JpaRepository<PlayerStats, Long> {

    @EntityGraph(attributePaths = {"player", "season"})
    @Query("SELECT ps FROM PlayerStats ps WHERE ps.player = :player ORDER BY ps.season.year DESC")
    List<PlayerStats> findByPlayer(Player player);

    @EntityGraph(attributePaths = {"player", "season"})
    Optional<PlayerStats> findByPlayerAndSeason(Player player, Season season);

    @Modifying
    @Query("DELETE FROM PlayerStats ps WHERE ps.player = :player")
    void deleteByPlayer(Player player);
}
