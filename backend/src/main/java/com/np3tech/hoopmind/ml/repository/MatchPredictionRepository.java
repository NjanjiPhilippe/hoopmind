package com.np3tech.hoopmind.ml.repository;

import com.np3tech.hoopmind.nba.entity.Game;
import com.np3tech.hoopmind.ml.entity.MatchPrediction;
import com.np3tech.hoopmind.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MatchPredictionRepository extends JpaRepository<MatchPrediction, Long> {

    Optional<MatchPrediction> findByGame(Game game);

    List<MatchPrediction> findByModel(String model);

    List<MatchPrediction> findByModelAndModelVersion(String model, String modelVersion);

    @Query("SELECT mp FROM MatchPrediction mp WHERE mp.game = :game ORDER BY mp.predictionDate DESC")
    List<MatchPrediction> findByGameOrderByDateDesc(@Param("game") Game game);

    @Query("SELECT mp FROM MatchPrediction mp WHERE mp.isAccurate IS NOT NULL")
    List<MatchPrediction> findEvaluatedPredictions();

    @Query("SELECT mp FROM MatchPrediction mp WHERE mp.model = :model AND mp.isAccurate = true")
    List<MatchPrediction> findCorrectPredictionsByModel(@Param("model") String model);

    List<MatchPrediction> findByPredictionDateBetween(LocalDateTime start, LocalDateTime end);

    List<MatchPrediction> findByCreatedByUser(User user);
}
