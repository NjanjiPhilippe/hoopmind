package com.np3tech.hoopmind.nba.repository;

import com.np3tech.hoopmind.nba.entity.Game;
import com.np3tech.hoopmind.nba.entity.GameResult;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GameResultRepository extends JpaRepository<GameResult, Long> {

    Optional<GameResult> findByGame(Game game);

    /** Résultats d'une liste de matchs en une requête, avec l'équipe gagnante (affichée dans la liste des matchs). */
    @EntityGraph(attributePaths = {"game", "winner"})
    List<GameResult> findByGameIdIn(Collection<Long> gameIds);
}
