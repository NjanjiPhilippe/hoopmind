package com.np3tech.hoopmind.nba.repository;

import com.np3tech.hoopmind.nba.entity.Season;
import com.np3tech.hoopmind.nba.entity.Team;
import com.np3tech.hoopmind.nba.entity.TeamStats;
import com.np3tech.hoopmind.nba.entity.enummeration.Conference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamStatsRepository extends JpaRepository<TeamStats, Long> {

    Optional<TeamStats> findByTeamAndSeason(Team team, Season season);

    List<TeamStats> findBySeason(Season season);

    List<TeamStats> findByTeam(Team team);

    @Query("SELECT ts FROM TeamStats ts WHERE ts.season = :season ORDER BY ts.wins DESC, ts.winPercentage DESC")
    List<TeamStats> findStandingsBySeason(@Param("season") Season season);

    @Query("SELECT ts FROM TeamStats ts WHERE ts.season = :season AND ts.team.conference = :conference ORDER BY ts.conferenceRank")
    List<TeamStats> findConferenceStandings(@Param("season") Season season, @Param("conference") Conference conference);

    void deleteByTeam(Team team);
}