package com.np3tech.hoopmind.nba.dto;

import com.np3tech.hoopmind.ml.dto.*;

import com.np3tech.hoopmind.nba.entity.enummeration.MatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchDTO {
    private Long id;
    private Long homeTeamId;
    private String homeTeamName;
    private Long awayTeamId;
    private String awayTeamName;
    private LocalDateTime matchDate;
    private Integer homeScore;
    private Integer awayScore;
    private MatchStatus status;
    private String season;
    private String venue;
    private MatchPredictionDTO prediction;
}