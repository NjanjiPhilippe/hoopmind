package com.np3tech.hoopmind.nba.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameSummaryResponse {
    private Long id;
    private TeamSummaryResponse homeTeam;
    private TeamSummaryResponse awayTeam;
    private LocalDateTime scheduledDate;
}
