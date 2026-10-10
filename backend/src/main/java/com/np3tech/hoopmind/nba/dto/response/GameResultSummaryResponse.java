package com.np3tech.hoopmind.nba.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameResultSummaryResponse {
    private Integer homeTeamScore;
    private Integer awayTeamScore;
    private TeamSummaryResponse winner;
    private Boolean overtime;
}
