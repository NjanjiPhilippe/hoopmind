package com.np3tech.hoopmind.nba.repository;

import com.np3tech.hoopmind.nba.entity.Game;
import com.np3tech.hoopmind.nba.entity.Season;
import com.np3tech.hoopmind.nba.entity.Team;
import com.np3tech.hoopmind.nba.entity.enummeration.GameStatus;
import com.np3tech.hoopmind.nba.entity.enummeration.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long>, JpaSpecificationExecutor<Game> {
    List<Game> findByStatus(GameStatus status);

    List<Game> findBySeason(Season season);
    List<Game> findByStatusAndScheduledDate(GameStatus status, LocalDateTime now);

    List<Game> findByHomeTeamOrAwayTeam(Team homeTeam, Team awayTeam);

    List<Game> findByScheduledDateBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT g FROM Game g WHERE g.season = :season AND (g.homeTeam = :team OR g.awayTeam = :team)")
    List<Game> findBySeasonAndTeam(@Param("season") Season season, @Param("team") Team team);

    @Query("SELECT g FROM Game g WHERE g.status = :status AND g.scheduledDate < :date")
    List<Game> findByStatusAndScheduledDateBefore(@Param("status") GameStatus status, @Param("date") LocalDateTime date);

    Optional<Game> findByHomeTeamAndAwayTeamAndScheduledDate(Team homeTeam, Team awayTeam, LocalDateTime scheduledDate);

}
