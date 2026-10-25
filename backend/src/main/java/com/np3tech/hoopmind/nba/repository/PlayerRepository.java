package com.np3tech.hoopmind.nba.repository;

import com.np3tech.hoopmind.nba.entity.Player;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

import com.np3tech.hoopmind.nba.entity.Player;
import com.np3tech.hoopmind.nba.entity.Team;
import com.np3tech.hoopmind.nba.entity.enummeration.PlayerStatus;
import com.np3tech.hoopmind.nba.entity.enummeration.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Les lectures chargent l'équipe avec le joueur : la réponse est construite hors transaction
 * ({@code open-in-view: false}), un proxy non chargé y lèverait une LazyInitializationException.
 */
@Repository
public interface PlayerRepository extends JpaRepository<Player, Long>, JpaSpecificationExecutor<Player> {

    @Override
    @EntityGraph(attributePaths = "team")
    Optional<Player> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "team")
    Page<Player> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "team")
    List<Player> findByTeam(Team team);

    @EntityGraph(attributePaths = "team")
    Page<Player> findByPosition(Position position, Pageable pageable);

    @EntityGraph(attributePaths = "team")
    Page<Player> findByStatus(PlayerStatus status, Pageable pageable);

    List<Player> findByTeamAndStatus(Team team, PlayerStatus status);

    @EntityGraph(attributePaths = "team")
    @Query("SELECT p FROM Player p WHERE LOWER(p.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Player> searchByName(@Param("name") String name);

    Optional<Player> findByFirstNameAndLastNameAndTeam(String firstName, String lastName, Team team);

    List<Player> findByTeamAndJerseyNumber(Team team, Integer jerseyNumber);
}
