package com.np3tech.hoopmind.ml.service;

import com.np3tech.hoopmind.ml.dto.request.MatchPredictionCreateRequest;
import com.np3tech.hoopmind.nba.entity.Game;
import com.np3tech.hoopmind.nba.entity.GameResult;
import com.np3tech.hoopmind.nba.entity.Team;
import com.np3tech.hoopmind.ml.entity.MatchPrediction;
import com.np3tech.hoopmind.shared.exception.AppException;
import com.np3tech.hoopmind.nba.repository.GameResultRepository;
import com.np3tech.hoopmind.ml.repository.MatchPredictionRepository;
import com.np3tech.hoopmind.nba.service.GameService;
import com.np3tech.hoopmind.nba.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MatchPredictionService {

    private final MatchPredictionRepository matchPredictionRepository;
    private final GameService gameService;
    private final TeamService teamService;
    private final GameResultRepository gameResultRepository;
    private final Clock clock;

    public MatchPrediction createPrediction(MatchPredictionCreateRequest request) {
        Game game = gameService.getGameById(request.getGameId());
        Team predictedWinner = teamService.getTeamById(request.getPredictedWinnerId());

        MatchPrediction prediction = MatchPrediction.builder()
                .game(game)
                .predictedWinner(predictedWinner)
                .winProbability(request.getWinProbability())
                .predictedHomeScore(request.getPredictedHomeScore())
                .predictedAwayScore(request.getPredictedAwayScore())
                .predictedScoreDifference(request.getPredictedScoreDifference())
                .model(request.getModel())
                .modelVersion(request.getModelVersion())
                .predictionFactorsJson(request.getPredictionFactorsJson())
                .confidence(request.getConfidence())
                .build();

        log.info("Creating match prediction for game id: {}", prediction.getGame().getId());

        validatePrediction(prediction);

        prediction.setPredictionDate(LocalDateTime.now(clock));
        prediction.setLastUpdated(LocalDateTime.now(clock));

        return matchPredictionRepository.save(prediction);
    }

    public MatchPrediction updatePrediction(Long id, MatchPrediction predictionUpdate) {
        log.info("Updating match prediction with id: {}", id);

        MatchPrediction existing = getPredictionById(id);

        if (predictionUpdate.getWinProbability() != null) {
            existing.setWinProbability(predictionUpdate.getWinProbability());
        }
        if (predictionUpdate.getPredictedHomeScore() != null) {
            existing.setPredictedHomeScore(predictionUpdate.getPredictedHomeScore());
        }
        if (predictionUpdate.getPredictedAwayScore() != null) {
            existing.setPredictedAwayScore(predictionUpdate.getPredictedAwayScore());
        }
        if (predictionUpdate.getConfidence() != null) {
            existing.setConfidence(predictionUpdate.getConfidence());
        }

        existing.setLastUpdated(LocalDateTime.now(clock));

        return matchPredictionRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public MatchPrediction getPredictionById(Long id) {
        return matchPredictionRepository.findById(id)
                .orElseThrow(() -> new AppException(
                        "Match prediction not found with id: " + id,
                        HttpStatus.NOT_FOUND,
                        "MATCH_PREDICTION_NOT_FOUND"
                ));
    }

    @Transactional(readOnly = true)
    public MatchPrediction getPredictionByGame(Long gameId) {
        Game game = gameService.getGameById(gameId);
        return matchPredictionRepository.findByGame(game)
                .orElseThrow(() -> new AppException(
                        "Match prediction not found for game id: " + gameId,
                        HttpStatus.NOT_FOUND,
                        "MATCH_PREDICTION_NOT_FOUND"
                ));
    }

    @Transactional(readOnly = true)
    public List<MatchPrediction> getPredictionsByModel(String model) {
        return matchPredictionRepository.findByModel(model);
    }

    @Transactional(readOnly = true)
    public List<MatchPrediction> getEvaluatedPredictions() {
        return matchPredictionRepository.findEvaluatedPredictions();
    }

    public void evaluatePrediction(Long predictionId) {
        log.info("Evaluating prediction with id: {}", predictionId);

        MatchPrediction prediction = getPredictionById(predictionId);
        Game game = prediction.getGame();

        GameResult result = gameResultRepository.findByGame(game)
                .orElseThrow(() -> new AppException(
                        "Cannot evaluate prediction: game result not found for game id: " + game.getId(),
                        HttpStatus.NOT_FOUND,
                        "GAME_RESULT_NOT_FOUND"
                ));

        // Evaluate winner prediction
        boolean winnerCorrect = prediction.getPredictedWinner().getId().equals(result.getWinner().getId());
        prediction.setPredictedWinnerCorrect(winnerCorrect);

        // Calculate prediction error if scores were predicted
        if (prediction.getPredictedHomeScore() != null && prediction.getPredictedAwayScore() != null) {
            int predictedDiff = Math.abs(prediction.getPredictedHomeScore() - prediction.getPredictedAwayScore());
            int actualDiff = result.getScoreDifference();

            BigDecimal error = BigDecimal.valueOf(Math.abs(predictedDiff - actualDiff));
            prediction.setPredictionError(error);
        }

        prediction.setActualScoreDifference(result.getScoreDifference());
        prediction.setIsAccurate(winnerCorrect);
        prediction.setEvaluatedAt(LocalDateTime.now(clock));

        matchPredictionRepository.save(prediction);
    }

    public void deletePrediction(Long id) {
        log.info("Deleting match prediction with id: {}", id);

        if (!matchPredictionRepository.existsById(id)) {
            throw new AppException(
                    "Match prediction not found with id: " + id,
                    HttpStatus.NOT_FOUND,
                    "MATCH_PREDICTION_NOT_FOUND"
            );
        }

        matchPredictionRepository.deleteById(id);
    }

    private void validatePrediction(MatchPrediction prediction) {
        if (prediction.getGame() == null) {
            throw new AppException(
                    "Game is required for match prediction",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PREDICTION_DATA"
            );
        }

        if (prediction.getPredictedWinner() == null) {
            throw new AppException(
                    "Predicted winner is required",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PREDICTION_DATA"
            );
        }

        if (prediction.getModel() == null || prediction.getModel().trim().isEmpty()) {
            throw new AppException(
                    "Model name is required",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PREDICTION_DATA"
            );
        }
    }
}
