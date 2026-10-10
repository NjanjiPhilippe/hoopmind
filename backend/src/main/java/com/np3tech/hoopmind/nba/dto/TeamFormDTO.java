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
public class TeamFormDTO {
    private Long teamId;
    private String teamName;
    private int matchesAnalyzed;
    private int wins;
    private int losses;
    private double winRate;
    private List<String> form; // ["W", "L", "W", "W", "L"]
    private double averagePointsScored;
    private double averagePointsAllowed;
    private double pointDifferential;
    private boolean isOnWinStreak;
}