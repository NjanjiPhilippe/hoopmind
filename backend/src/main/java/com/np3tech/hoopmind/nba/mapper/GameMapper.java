package com.np3tech.hoopmind.nba.mapper;

import com.np3tech.hoopmind.nba.dto.request.GameCreateRequest;
import com.np3tech.hoopmind.nba.dto.request.GameUpdateRequest;
import com.np3tech.hoopmind.nba.dto.response.*;
import com.np3tech.hoopmind.nba.entity.*;
import com.np3tech.hoopmind.nba.service.SeasonService;
import com.np3tech.hoopmind.nba.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameMapper {

    private final TeamService teamService;
    private final SeasonService seasonService;

    public Game toEntity(GameCreateRequest request) {
        Team homeTeam = teamService.getTeamById(request.getHomeTeamId());
        Team awayTeam = teamService.getTeamById(request.getAwayTeamId());
        Season season = seasonService.getSeasonById(request.getSeasonId());

        return Game.builder()
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .season(season)
                .gameType(request.getGameType())
                .scheduledDate(request.getScheduledDate())
                .venue(request.getVenue())
                .isNationalTv(request.getIsNationalTv())
                .broadcastNetwork(request.getBroadcastNetwork())
                .build();
    }

    public Game toEntity(GameUpdateRequest request) {
        return Game.builder()
                .scheduledDate(request.getScheduledDate())
                .actualStartTime(request.getActualStartTime())
                .actualEndTime(request.getActualEndTime())
                .venue(request.getVenue())
                .attendance(request.getAttendance())
                .isNationalTv(request.getIsNationalTv())
                .broadcastNetwork(request.getBroadcastNetwork())
                .build();
    }

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
