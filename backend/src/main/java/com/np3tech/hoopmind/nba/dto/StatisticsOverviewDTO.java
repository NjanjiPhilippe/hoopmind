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
public class StatisticsOverviewDTO {
    private Integer season;
    private TeamStatisticDTO teamStats;
    private List<PlayerStatisticDTO> topScorers;
    private List<PlayerStatisticDTO> topRebounders;
    private List<PlayerStatisticDTO> topAssistants;
    private TeamFormDTO recentForm;
}
