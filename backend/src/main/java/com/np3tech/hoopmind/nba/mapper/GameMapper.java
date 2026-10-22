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
                .homeTeam(toTeamSummary(game.getHomeTeam()))
                .awayTeam(toTeamSummary(game.getAwayTeam()))
                .season(toSeasonSummary(game.getSeason()))
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
                    .winner(toTeamSummary(result.getWinner()))
                    .overtime(result.getOvertime())
                    .build());
        }

        return builder.build();
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

    private SeasonSummaryResponse toSeasonSummary(Season season) {
        return SeasonSummaryResponse.builder()
                .id(season.getId())
                .year(season.getYear())
                .name(season.getName())
                .build();
    }
}
