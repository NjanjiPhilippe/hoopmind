export interface Team {
  id: number;
  name: string;
  city: string;
  abbreviation: string;
  conference: string;
  division: string;
  logoUrl?: string;
}

export interface TeamStatistic {
  id: number;
  teamId: number;
  teamName: string;
  season: string;
  wins: number;
  losses: number;
  winPercentage: number;
  pointsPerGame: number;
  pointsAllowedPerGame: number;
  fieldGoalPercentage: number;
  threePointPercentage: number;
  freeThrowPercentage: number;
  reboundsPerGame: number;
  assistsPerGame: number;
  homeRecord: string;
  awayRecord: string;
}

export interface TeamForm {
  teamId: number;
  teamName: string;
  matchesAnalyzed: number;
  wins: number;
  losses: number;
  winRate: number;
  form: string[]; // ["W", "L", "W", "W", "L"]
  averagePointsScored: number;
  averagePointsAllowed: number;
  pointDifferential: number;
  isOnWinStreak: boolean;
}