package com.np3tech.hoopmind.nba.repository;

import com.np3tech.hoopmind.nba.entity.Season;
import com.np3tech.hoopmind.nba.entity.TeamStats;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Les lectures chargent l'équipe et la saison : la réponse est construite hors session JPA. */
public interface TeamStatsRepository extends JpaRepository<TeamStats, Long> {

    @EntityGraph(attributePaths = {"team", "season"})
    @Query("SELECT ts FROM TeamStats ts WHERE ts.season = :season ORDER BY ts.wins DESC, ts.winPercentage DESC")
    List<TeamStats> findStandingsBySeason(@Param("season") Season season);
}
