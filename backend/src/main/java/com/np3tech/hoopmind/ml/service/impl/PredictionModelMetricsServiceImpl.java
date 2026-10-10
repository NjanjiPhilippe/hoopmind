package com.np3tech.hoopmind.ml.service.impl;

import com.np3tech.hoopmind.ml.entity.PredictionModelMetrics;
import com.np3tech.hoopmind.shared.exception.AppException;
import com.np3tech.hoopmind.ml.repository.PredictionModelMetricsRepository;
import com.np3tech.hoopmind.ml.service.PredictionModelMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PredictionModelMetricsServiceImpl implements PredictionModelMetricsService {

    private final PredictionModelMetricsRepository metricsRepository;

    public PredictionModelMetrics createMetrics(PredictionModelMetrics metrics) {
        log.info("Creating prediction model metrics for model: {}", metrics.getModel());

        validateMetrics(metrics);

        return metricsRepository.save(metrics);
    }

    public PredictionModelMetrics updateMetrics(Long id, PredictionModelMetrics metricsUpdate) {
        log.info("Updating prediction model metrics with id: {}", id);

        PredictionModelMetrics existing = getMetricsById(id);

        if (metricsUpdate.getTotalPredictions() != null) {
            existing.setTotalPredictions(metricsUpdate.getTotalPredictions());
        }
        if (metricsUpdate.getCorrectPredictions() != null) {
            existing.setCorrectPredictions(metricsUpdate.getCorrectPredictions());
        }
        if (metricsUpdate.getAccuracy() != null) {
            existing.setAccuracy(metricsUpdate.getAccuracy());
        }
        if (metricsUpdate.getPrecisionScore() != null) {
            existing.setPrecisionScore(metricsUpdate.getPrecisionScore());
        }
        if (metricsUpdate.getRecall() != null) {
            existing.setRecall(metricsUpdate.getRecall());
        }
        if (metricsUpdate.getF1Score() != null) {
            existing.setF1Score(metricsUpdate.getF1Score());
        }

        return metricsRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public PredictionModelMetrics getMetricsById(Long id) {
        return metricsRepository.findById(id)
                .orElseThrow(() -> new AppException(
                        "Prediction model metrics not found with id: " + id,
                        HttpStatus.NOT_FOUND,
                        "METRICS_NOT_FOUND"
                ));
    }

    @Transactional(readOnly = true)
    public List<PredictionModelMetrics> getMetricsByModel(String model) {
        return metricsRepository.findByModelOrderByDateDesc(model);
    }

    @Transactional(readOnly = true)
    public List<PredictionModelMetrics> getMetricsByDateRange(LocalDate start, LocalDate end) {
        return metricsRepository.findByEvaluationDateBetween(start, end);
    }

    @Transactional(readOnly = true)
    public List<PredictionModelMetrics> getAllMetricsOrderedByAccuracy() {
        return metricsRepository.findAllOrderByAccuracyDesc();
    }

    public void deleteMetrics(Long id) {
        log.info("Deleting prediction model metrics with id: {}", id);

        if (!metricsRepository.existsById(id)) {
            throw new AppException(
                    "Prediction model metrics not found with id: " + id,
                    HttpStatus.NOT_FOUND,
                    "METRICS_NOT_FOUND"
            );
        }

        metricsRepository.deleteById(id);
    }

    private void validateMetrics(PredictionModelMetrics metrics) {
        if (metrics.getModel() == null || metrics.getModel().trim().isEmpty()) {
            throw new AppException(
                    "Model name is required",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_METRICS_DATA"
            );
        }

        if (metrics.getModelVersion() == null || metrics.getModelVersion().trim().isEmpty()) {
            throw new AppException(
                    "Model version is required",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_METRICS_DATA"
            );
        }

        if (metrics.getEvaluationDate() == null) {
            throw new AppException(
                    "Evaluation date is required",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_METRICS_DATA"
            );
        }
    }
}