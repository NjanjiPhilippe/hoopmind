package com.np3tech.hoopmind.nba.dto.response;

import com.np3tech.hoopmind.nba.entity.enummeration.GameStatus;
import com.np3tech.hoopmind.nba.entity.enummeration.GameType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameResponse {
    private Long id;
    private TeamSummaryResponse homeTeam;
    private TeamSummaryResponse awayTeam;
    private SeasonSummaryResponse season;
    private GameType gameType;
    private LocalDateTime scheduledDate;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
    private String venue;
    private Integer attendance;
    private GameStatus status;
    private Boolean isNationalTv;
    private String broadcastNetwork;
    private GameResultSummaryResponse result;
}

