package com.np3tech.hoopmind.ml.mapper;

import com.np3tech.hoopmind.ml.dto.request.MatchPredictionCreateRequest;
import com.np3tech.hoopmind.nba.dto.response.GameSummaryResponse;
import com.np3tech.hoopmind.ml.dto.response.MatchPredictionResponse;
import com.np3tech.hoopmind.nba.dto.response.TeamSummaryResponse;
import com.np3tech.hoopmind.nba.entity.Game;
import com.np3tech.hoopmind.ml.entity.MatchPrediction;
import com.np3tech.hoopmind.nba.entity.Team;
import com.np3tech.hoopmind.nba.service.GameService;
import com.np3tech.hoopmind.nba.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchPredictionMapper {

    private final GameService gameService;
    private final TeamService teamService;

    public MatchPrediction toEntity(MatchPredictionCreateRequest request) {
        Game game = gameService.getGameById(request.getGameId());
        Team predictedWinner = teamService.getTeamById(request.getPredictedWinnerId());

        return MatchPrediction.builder()
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
    }

    public MatchPredictionResponse toResponse(MatchPrediction prediction) {
        return MatchPredictionResponse.builder()
                .id(prediction.getId())
                .game(toGameSummary(prediction.getGame()))
                .predictedWinner(toTeamSummary(prediction.getPredictedWinner()))
                .winProbability(prediction.getWinProbability())
                .predictedHomeScore(prediction.getPredictedHomeScore())
                .predictedAwayScore(prediction.getPredictedAwayScore())
                .predictedScoreDifference(prediction.getPredictedScoreDifference())
                .model(prediction.getModel())
                .modelVersion(prediction.getModelVersion())
                .confidence(prediction.getConfidence())
                .predictionDate(prediction.getPredictionDate())
                .isAccurate(prediction.getIsAccurate())
                .predictedWinnerCorrect(prediction.getPredictedWinnerCorrect())
                .actualScoreDifference(prediction.getActualScoreDifference())
                .predictionError(prediction.getPredictionError())
                .build();
    }

    private GameSummaryResponse toGameSummary(Game game) {
        return GameSummaryResponse.builder()
                .id(game.getId())
                .homeTeam(toTeamSummary(game.getHomeTeam()))
                .awayTeam(toTeamSummary(game.getAwayTeam()))
                .scheduledDate(game.getScheduledDate())
                .build();
    }

    private TeamSummaryResponse toTeamSummary(Team team) {
        return TeamSummaryResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .city(team.getCity())
                .abbreviation(team.getAbbreviation())
                .logoUrl(team.getLogoUrl())
                .build();
    }
}
