package com.np3tech.hoopmind.ml.service;

import com.np3tech.hoopmind.ml.entity.PredictionModelMetrics;

import java.time.LocalDate;
import java.util.List;

public interface PredictionModelMetricsService {
    PredictionModelMetrics createMetrics(PredictionModelMetrics metrics);
    PredictionModelMetrics updateMetrics(Long id, PredictionModelMetrics metricsUpdate);
    PredictionModelMetrics getMetricsById(Long id);
    List<PredictionModelMetrics> getMetricsByModel(String model);
    List<PredictionModelMetrics> getMetricsByDateRange(LocalDate start, LocalDate end);
    List<PredictionModelMetrics> getAllMetricsOrderedByAccuracy();
    void deleteMetrics(Long id);
}
