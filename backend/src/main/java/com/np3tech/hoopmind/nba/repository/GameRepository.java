package com.np3tech.hoopmind.nba.repository;

import com.np3tech.hoopmind.nba.entity.Game;
import com.np3tech.hoopmind.nba.entity.Season;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Les lectures chargent les équipes et la saison avec le match : la session JPA est fermée quand le contrôleur
 * construit la réponse ({@code open-in-view: false}), un proxy non chargé y lèverait une LazyInitializationException.
 */
public interface GameRepository extends JpaRepository<Game, Long> {

    @Override
    @EntityGraph(attributePaths = {"homeTeam", "awayTeam", "season"})
    Optional<Game> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"homeTeam", "awayTeam", "season"})
    Page<Game> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"homeTeam", "awayTeam", "season"})
    List<Game> findBySeason(Season season);
}
