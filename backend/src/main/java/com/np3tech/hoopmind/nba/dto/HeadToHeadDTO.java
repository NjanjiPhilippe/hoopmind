package com.np3tech.hoopmind.nba.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeadToHeadDTO {
    private Long team1Id;
    private String team1Name;
    private Long team2Id;
    private String team2Name;
    private int team1Wins;
    private int team2Wins;
    private int totalMatches;
    private double team1WinRate;
    private double averagePointDifferential;
    private List<MatchDTO> recentMatches;
}