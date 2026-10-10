package com.np3tech.hoopmind.nba.dto.request;

import com.np3tech.hoopmind.nba.entity.enummeration.GameType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameCreateRequest {

    @NotNull(message = "Home team ID is required")
    private Long homeTeamId;

    @NotNull(message = "Away team ID is required")
    private Long awayTeamId;

    @NotNull(message = "Season ID is required")
    private Long seasonId;

    @NotNull(message = "Game type is required")
    private GameType gameType;

    @NotNull(message = "Scheduled date is required")
    private LocalDateTime scheduledDate;

    private String venue;
    private Boolean isNationalTv;
    private String broadcastNetwork;
}
