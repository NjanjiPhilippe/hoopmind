package com.np3tech.hoopmind.nba.repository;

import com.np3tech.hoopmind.nba.entity.Player;
import com.np3tech.hoopmind.nba.entity.PlayerStats;
import com.np3tech.hoopmind.nba.entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerStatsRepository extends JpaRepository<PlayerStats, Long> {

    Optional<PlayerStats> findByPlayerAndSeason(Player player, Season season);

    List<PlayerStats> findBySeason(Season season);

    List<PlayerStats> findByPlayer(Player player);

    @Query("SELECT ps FROM PlayerStats ps WHERE ps.season = :season ORDER BY ps.pointsPerGame DESC")
    List<PlayerStats> findTopScorers(@Param("season") Season season);

    @Query("SELECT ps FROM PlayerStats ps WHERE ps.season = :season ORDER BY ps.assistsPerGame DESC")
    List<PlayerStats> findTopAssists(@Param("season") Season season);

    @Query("SELECT ps FROM PlayerStats ps WHERE ps.season = :season ORDER BY ps.totalReboundsPerGame DESC")
    List<PlayerStats> findTopRebounders(@Param("season") Season season);

    void deleteByPlayer(Player player);
}
