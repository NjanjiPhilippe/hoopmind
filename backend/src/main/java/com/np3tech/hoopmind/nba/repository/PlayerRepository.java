package com.np3tech.hoopmind.nba.repository;

import com.np3tech.hoopmind.nba.entity.Player;
import com.np3tech.hoopmind.nba.entity.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Les lectures chargent l'équipe avec le joueur : la réponse est construite hors transaction
 * ({@code open-in-view: false}), un proxy non chargé y lèverait une LazyInitializationException.
 */
public interface PlayerRepository extends JpaRepository<Player, Long> {

    @Override
    @EntityGraph(attributePaths = "team")
    Optional<Player> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "team")
    Page<Player> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "team")
    List<Player> findByTeam(Team team);
}
