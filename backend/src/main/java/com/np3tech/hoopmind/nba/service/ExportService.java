package com.np3tech.hoopmind.nba.service;

import com.np3tech.hoopmind.nba.entity.Game;
import com.np3tech.hoopmind.nba.entity.PlayerStats;
import com.np3tech.hoopmind.nba.entity.TeamStats;

import java.util.List;

public interface ExportService {
    byte[] exportTeamStatsToCSV(List<TeamStats> stats);
    byte[] exportPlayerStatsToCSV(List<PlayerStats> stats);
    byte[] exportGameScheduleToCSV(List<Game> games);
}
