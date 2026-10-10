package com.np3tech.hoopmind.nba.repository;

import com.np3tech.hoopmind.nba.entity.TeamStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamStatisticRepository extends JpaRepository<TeamStats, Long> {
    List<TeamStats> findBySeason_Year(Integer season);
    Optional<TeamStats> findByTeamIdAndSeason_Year(Long teamId, Integer season);

    @Query("SELECT ts FROM TeamStats ts WHERE ts.season.year = :season " +
            "ORDER BY ts.winPercentage DESC")
    List<TeamStats> findTopTeamsBySeason(@Param("season") Integer season);

    @Query("SELECT ts FROM TeamStats ts WHERE ts.team.conference = :conference " +
            "AND ts.season.year = :season ORDER BY ts.winPercentage DESC")
    List<TeamStats> findByConferenceAndSeason(@Param("conference") String conference,
                                              @Param("season") Integer season);
}
