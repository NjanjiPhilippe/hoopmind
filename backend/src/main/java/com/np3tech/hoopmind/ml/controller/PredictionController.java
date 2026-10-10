package com.np3tech.hoopmind.ml.controller;

import com.np3tech.hoopmind.ml.api.PredictionApi;
import com.np3tech.hoopmind.ml.mapper.MatchPredictionMapper;
import com.np3tech.hoopmind.ml.dto.request.MatchPredictionCreateRequest;
import com.np3tech.hoopmind.ml.dto.response.MatchPredictionResponse;
import com.np3tech.hoopmind.ml.entity.MatchPrediction;
import com.np3tech.hoopmind.ml.service.MatchPredictionService;
import com.np3tech.hoopmind.nba.service.ReportingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PredictionController implements PredictionApi {

    private final MatchPredictionService matchPredictionService;
    private final ReportingService reportingService;
    private final MatchPredictionMapper matchPredictionMapper;

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<MatchPredictionResponse> createPrediction(MatchPredictionCreateRequest request) {
        log.info("Creating prediction for game: {}", request.getGameId());

        MatchPrediction prediction = matchPredictionMapper.toEntity(request);
        MatchPrediction createdPrediction = matchPredictionService.createPrediction(prediction);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(matchPredictionMapper.toResponse(createdPrediction));
    }

    @Override
    public ResponseEntity<MatchPredictionResponse> getPredictionById(Long id) {
        log.debug("Fetching prediction with id: {}", id);

        MatchPrediction prediction = matchPredictionService.getPredictionById(id);
        return ResponseEntity.ok(matchPredictionMapper.toResponse(prediction));
    }

    @Override
    public ResponseEntity<MatchPredictionResponse> getPredictionByGame(Long gameId) {
        log.debug("Fetching prediction for game: {}", gameId);

        MatchPrediction prediction = matchPredictionService.getPredictionByGame(gameId);
        return ResponseEntity.ok(matchPredictionMapper.toResponse(prediction));
    }

    @Override
    public ResponseEntity<List<MatchPredictionResponse>> getPredictionsByModel(String model) {
        log.debug("Fetching predictions for model: {}", model);

        List<MatchPrediction> predictions = matchPredictionService.getPredictionsByModel(model);
        List<MatchPredictionResponse> responses = predictions.stream()
                .map(matchPredictionMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<List<MatchPredictionResponse>> getEvaluatedPredictions() {
        log.debug("Fetching evaluated predictions");

        List<MatchPrediction> predictions = matchPredictionService.getEvaluatedPredictions();
        List<MatchPredictionResponse> responses = predictions.stream()
                .map(matchPredictionMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MatchPredictionResponse> evaluatePrediction(Long id) {
        log.info("Evaluating prediction with id: {}", id);

        matchPredictionService.evaluatePrediction(id);
        MatchPrediction prediction = matchPredictionService.getPredictionById(id);

        return ResponseEntity.ok(matchPredictionMapper.toResponse(prediction));
    }

    @Override
    public ResponseEntity<?> getPredictionAccuracyReport(String model) {
        log.debug("Generating accuracy report for model: {}", model);

        Map<String, Object> report = reportingService.generatePredictionAccuracyReport(model);
        return ResponseEntity.ok(report);
    }
}