package com.np3tech.hoopmind.ml.history;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

interface SavedPredictionRepository extends JpaRepository<SavedPrediction, Long> {

    Page<SavedPrediction> findByUserId(long userId, Pageable pageable);

    Optional<SavedPrediction> findByIdAndUserId(long id, long userId);

    Optional<SavedPrediction> findByUserIdAndSeasonIdAndHomeTeamIdAndAwayTeamIdAndGameDateAndPlayoff(
            long userId, long seasonId, long homeTeamId, long awayTeamId, LocalDate gameDate, boolean playoff);
}
