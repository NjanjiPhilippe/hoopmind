package com.np3tech.hoopmind.ml.mapper;

import com.np3tech.hoopmind.nba.dto.response.GameSummaryResponse;
import com.np3tech.hoopmind.ml.dto.response.MatchPredictionResponse;
import com.np3tech.hoopmind.nba.dto.response.TeamSummaryResponse;
import com.np3tech.hoopmind.nba.entity.Game;
import com.np3tech.hoopmind.ml.entity.MatchPrediction;
import com.np3tech.hoopmind.nba.entity.Team;
import org.springframework.stereotype.Component;

@Component
public class MatchPredictionMapper {

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
