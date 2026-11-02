package com.np3tech.hoopmind.nba.repository;

import com.np3tech.hoopmind.nba.entity.Game;
import com.np3tech.hoopmind.nba.entity.Season;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Les lectures chargent les équipes et la saison avec le match : la session JPA est fermée quand le contrôleur
 * construit la réponse ({@code open-in-view: false}), un proxy non chargé y lèverait une LazyInitializationException.
 */
public interface GameRepository extends JpaRepository<Game, Long> {

    /**
     * Verrouille la ligne du match jusqu'à la fin de la transaction : deux saisies du même score passent l'une après
     * l'autre. Requête native : PostgreSQL refuse {@code FOR UPDATE} sur les jointures externes du graphe d'entités,
     * et le match est relu ensuite avec ses équipes par {@link #findById}.
     */
    @Query(value = "SELECT id FROM games WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<Long> lockById(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = {"homeTeam", "awayTeam", "season"})
    Optional<Game> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"homeTeam", "awayTeam", "season"})
    Page<Game> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"homeTeam", "awayTeam", "season"})
    List<Game> findBySeason(Season season);
}
