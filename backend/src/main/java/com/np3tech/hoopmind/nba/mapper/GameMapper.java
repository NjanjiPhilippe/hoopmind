package com.np3tech.hoopmind.nba.mapper;

import com.np3tech.hoopmind.nba.dto.response.*;
import com.np3tech.hoopmind.nba.entity.*;
import org.springframework.stereotype.Component;

@Component
public class GameMapper {

    public GameResponse toResponse(Game game) {
        return toResponse(game, null);
    }

    /** @param result résultat du match s'il a été joué, sinon {@code null} */
    public GameResponse toResponse(Game game, GameResult result) {
        GameResponse.GameResponseBuilder builder = GameResponse.builder()
                .id(game.getId())
                .homeTeam(TeamSummaryResponse.of(game.getHomeTeam()))
                .awayTeam(TeamSummaryResponse.of(game.getAwayTeam()))
                .season(SeasonSummaryResponse.of(game.getSeason()))
                .gameType(game.getGameType())
                .scheduledDate(game.getScheduledDate())
                .actualStartTime(game.getActualStartTime())
                .actualEndTime(game.getActualEndTime())
                .venue(game.getVenue())
                .attendance(game.getAttendance())
                .status(game.getStatus())
                .isNationalTv(game.getIsNationalTv())
                .broadcastNetwork(game.getBroadcastNetwork());
        if (result != null) {
            builder.result(GameResultSummaryResponse.builder()
                    .homeTeamScore(result.getHomeTeamScore())
                    .awayTeamScore(result.getAwayTeamScore())
                    .winner(TeamSummaryResponse.of(result.getWinner()))
                    .overtime(result.getOvertime())
                    .build());
        }

        return builder.build();
    }
}
