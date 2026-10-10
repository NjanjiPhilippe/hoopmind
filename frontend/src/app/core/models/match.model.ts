export enum MatchStatus {
  SCHEDULED = 'SCHEDULED',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED'
}

export interface Match {
  id: number;
  homeTeamId: number;
  homeTeamName: string;
  awayTeamId: number;
  awayTeamName: string;
  matchDate: Date;
  homeScore?: number;
  awayScore?: number;
  status: MatchStatus;
  season: string;
  venue?: string;
  prediction?: MatchPrediction;
}

export interface MatchPrediction {
  id: number;
  matchId: number;
  homeTeamName: string;
  awayTeamName: string;
  matchDate: Date;
  predictedHomeScore: number;
  predictedAwayScore: number;
  homeWinProbability: number;
  awayWinProbability: number;
  confidenceScore: number;
  predictionDate: Date;
  modelVersion: string;
  factors?: string; // JSON string
}

export interface HeadToHead {
  team1Id: number;
  team1Name: string;
  team2Id: number;
  team2Name: string;
  team1Wins: number;
  team2Wins: number;
  totalMatches: number;
  team1WinRate: number;
  averagePointDifferential: number;
  recentMatches: Match[];
}