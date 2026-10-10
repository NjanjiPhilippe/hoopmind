package com.np3tech.hoopmind.nba.repository;

import com.np3tech.hoopmind.nba.entity.Game;
import com.np3tech.hoopmind.nba.entity.Team;
import com.np3tech.hoopmind.nba.entity.TeamGameStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamGameStatsRepository extends JpaRepository<TeamGameStats, Long> {

    List<TeamGameStats> findByGame(Game game);

    List<TeamGameStats> findByTeam(Team team);

    Optional<TeamGameStats> findByTeamAndGame(Team team, Game game);

    @Query("SELECT tgs FROM TeamGameStats tgs WHERE tgs.team = :team ORDER BY tgs.game.scheduledDate DESC")
    List<TeamGameStats> findByTeamOrderByDateDesc(@Param("team") Team team);

    void deleteByGame(Game game);
}
