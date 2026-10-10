import { MatchPrediction } from "./match.model";
import { Player, PlayerStatistic } from "./player.model";
import { Team, TeamStatistic, TeamForm } from "./team.model";

export interface Dashboard {
  totalTeams: number;
  totalPlayers: number;
  upcomingMatches: number;
  totalPredictions: number;
  featuredPredictions: MatchPrediction[];
  topTeams: Team[];
  hotPlayers: Player[];
}

export interface StatisticsOverview {
  season: string;
  teamStats?: TeamStatistic;
  topScorers: PlayerStatistic[];
  topRebounders: PlayerStatistic[];
  topAssistants: PlayerStatistic[];
  recentForm?: TeamForm;
}