package com.np3tech.hoopmind.nba.repository;

import com.np3tech.hoopmind.nba.entity.PlayerStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerStatisticRepository extends JpaRepository<PlayerStats, Long> {
    List<PlayerStats> findByPlayerId(Long playerId);
    List<PlayerStats> findBySeason_Year(Integer season);
    Optional<PlayerStats> findByPlayerIdAndSeason_Year(Long playerId, Integer season);

    @Query("SELECT ps FROM PlayerStats ps WHERE ps.player.team.id = :teamId AND ps.season.year = :season")
    List<PlayerStats> findByTeamAndSeason(@Param("teamId") Long teamId,
                                          @Param("season") Integer season);

    @Query("SELECT ps FROM PlayerStats ps WHERE ps.season.year = :season " +
            "ORDER BY ps.pointsPerGame DESC")
    List<PlayerStats> findTopScorers(@Param("season") Integer season);

    @Query("SELECT ps FROM PlayerStats ps WHERE ps.season.year = :season " +
            "ORDER BY ps.assistsPerGame DESC")
    List<PlayerStats> findTopAssistants(@Param("season") Integer season);
}