package com.np3tech.hoopmind.nba.repository;

import com.np3tech.hoopmind.nba.entity.Game;
import com.np3tech.hoopmind.nba.entity.GameResult;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GameResultRepository extends JpaRepository<GameResult, Long> {

    Optional<GameResult> findByGame(Game game);

    /** Résultats d'une liste de matchs en une requête, avec l'équipe gagnante (affichée dans la liste des matchs). */
    @EntityGraph(attributePaths = {"game", "winner"})
    List<GameResult> findByGameIdIn(Collection<Long> gameIds);

    /**
     * Détache les feuilles de match d'un résultat avant de le supprimer : elles restent liées au match, et un import
     * ou une nouvelle saisie les rattachera.
     */
    @Modifying
    @Query(value = "UPDATE team_game_stats SET game_result_id = NULL WHERE game_result_id = :resultId", nativeQuery = true)
    void detachTeamGameStats(@Param("resultId") Long resultId);

    @Modifying
    @Query(value = "UPDATE player_game_stats SET game_result_id = NULL WHERE game_result_id = :resultId", nativeQuery = true)
    void detachPlayerGameStats(@Param("resultId") Long resultId);
}
