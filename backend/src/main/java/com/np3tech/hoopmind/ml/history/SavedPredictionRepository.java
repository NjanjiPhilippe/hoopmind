package com.np3tech.hoopmind.ml.history;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

interface SavedPredictionRepository extends JpaRepository<SavedPrediction, Long> {

    Page<SavedPrediction> findByUserId(long userId, Pageable pageable);

    Optional<SavedPrediction> findByIdAndUserId(long id, long userId);

    Optional<SavedPrediction> findByUserIdAndSeasonIdAndHomeTeamIdAndAwayTeamIdAndGameDateAndPlayoff(
            long userId, long seasonId, long homeTeamId, long awayTeamId, LocalDate gameDate, boolean playoff);

    /** Supprime les prédictions de l'utilisateur au-delà des {@code keep} plus récentes. */
    @Modifying
    @Query(value = """
            DELETE FROM prediction_history
             WHERE user_id = :userId
               AND id NOT IN (SELECT id FROM prediction_history WHERE user_id = :userId
                              ORDER BY predicted_at DESC, id DESC LIMIT :keep)""", nativeQuery = true)
    int deleteOlderThanLatest(@Param("userId") long userId, @Param("keep") int keep);
}
