export interface AdvancedMetrics {
  avgPointsPerGame: number;
  avgReboundsPerGame: number;
  avgAssistsPerGame: number;
  avgFieldGoalPct: number;
  avgThreePointPct: number;
  totalTeamPoints: number;
  avgPER: number;
}

export interface TeamPatterns {
  avgHomeScore?: number;
  avgHomeScoreAllowed?: number;
  homeGames?: number;
  avgAwayScore?: number;
  avgAwayScoreAllowed?: number;
  awayGames?: number;
}