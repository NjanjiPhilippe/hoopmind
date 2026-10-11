package com.np3tech.hoopmind.nba.repository;

import com.np3tech.hoopmind.nba.entity.Game;
import com.np3tech.hoopmind.nba.entity.Season;
import com.np3tech.hoopmind.nba.entity.Team;
import com.np3tech.hoopmind.nba.entity.enummeration.GameStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
/**
 * Les lectures chargent les équipes et la saison avec le match : la session JPA est fermée quand le contrôleur
 * construit la réponse ({@code open-in-view: false}), un proxy non chargé y lèverait une LazyInitializationException.
 */
public interface GameRepository extends JpaRepository<Game, Long>, JpaSpecificationExecutor<Game> {
    @Override
    @EntityGraph(attributePaths = {"homeTeam", "awayTeam", "season"})
    Optional<Game> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"homeTeam", "awayTeam", "season"})
    List<Game> findAll();

    @Override
    @EntityGraph(attributePaths = {"homeTeam", "awayTeam", "season"})
    List<Game> findAll(Specification<Game> spec);

    @EntityGraph(attributePaths = {"homeTeam", "awayTeam", "season"})
    List<Game> findByStatus(GameStatus status);

    @EntityGraph(attributePaths = {"homeTeam", "awayTeam", "season"})
    List<Game> findBySeason(Season season);
    @EntityGraph(attributePaths = {"homeTeam", "awayTeam", "season"})
    List<Game> findByStatusAndScheduledDate(GameStatus status, LocalDateTime now);

    @EntityGraph(attributePaths = {"homeTeam", "awayTeam", "season"})
    List<Game> findByHomeTeamOrAwayTeam(Team homeTeam, Team awayTeam);

    @EntityGraph(attributePaths = {"homeTeam", "awayTeam", "season"})
    List<Game> findByScheduledDateBetween(LocalDateTime start, LocalDateTime end);

    @EntityGraph(attributePaths = {"homeTeam", "awayTeam", "season"})
    @Query("SELECT g FROM Game g WHERE g.season = :season AND (g.homeTeam = :team OR g.awayTeam = :team)")
    List<Game> findBySeasonAndTeam(@Param("season") Season season, @Param("team") Team team);

    @EntityGraph(attributePaths = {"homeTeam", "awayTeam", "season"})
    @Query("SELECT g FROM Game g WHERE g.status = :status AND g.scheduledDate < :date")
    List<Game> findByStatusAndScheduledDateBefore(@Param("status") GameStatus status, @Param("date") LocalDateTime date);

    Optional<Game> findByHomeTeamAndAwayTeamAndScheduledDate(Team homeTeam, Team awayTeam, LocalDateTime scheduledDate);

}
