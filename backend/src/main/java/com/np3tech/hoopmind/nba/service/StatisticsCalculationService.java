package com.np3tech.hoopmind.nba.service;

import com.np3tech.hoopmind.nba.entity.*;
import java.math.BigDecimal;

public interface StatisticsCalculationService {
    PlayerStats calculatePlayerSeasonStats(Player player, Season season);
    TeamStats calculateTeamSeasonStats(Team team, Season season);
    BigDecimal calculateWinProbability(Team homeTeam, Team awayTeam, Season season);
    BigDecimal calculatePER(PlayerGameStats stats);
}
