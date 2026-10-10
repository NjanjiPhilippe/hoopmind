package com.np3tech.hoopmind.ml.repository;

import com.np3tech.hoopmind.ml.entity.PredictionModelMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PredictionModelMetricsRepository extends JpaRepository<PredictionModelMetrics, Long> {

    List<PredictionModelMetrics> findByModel(String model);

    Optional<PredictionModelMetrics> findByModelAndModelVersionAndEvaluationDate(
            String model, String modelVersion, LocalDate evaluationDate);

    @Query("SELECT pmm FROM PredictionModelMetrics pmm WHERE pmm.model = :model ORDER BY pmm.evaluationDate DESC")
    List<PredictionModelMetrics> findByModelOrderByDateDesc(@Param("model") String model);

    @Query("SELECT pmm FROM PredictionModelMetrics pmm WHERE pmm.evaluationDate BETWEEN :start AND :end")
    List<PredictionModelMetrics> findByEvaluationDateBetween(
            @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT pmm FROM PredictionModelMetrics pmm ORDER BY pmm.accuracy DESC")
    List<PredictionModelMetrics> findAllOrderByAccuracyDesc();
}
