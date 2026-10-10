export interface Player {
  id: number;
  firstName: string;
  lastName: string;
  jerseyNumber?: number;
  position: string;
  dateOfBirth?: Date;
  height?: number;
  weight?: number;
  teamId?: number;
  photoUrl?: string;
  active: boolean;
}

export interface PlayerStatistic {
  id: number;
  playerId: number;
  playerName: string;
  season: string;
  gamesPlayed: number;
  minutesPerGame: number;
  pointsPerGame: number;
  reboundsPerGame: number;
  assistsPerGame: number;
  stealsPerGame: number;
  blocksPerGame: number;
  fieldGoalPercentage: number;
  threePointPercentage: number;
  freeThrowPercentage: number;
  turnoversPerGame: number;
  statDate?: Date;
}

export interface PlayerForm {
  playerId: number;
  playerName: string;
  season: string;
  gamesPlayed: number;
  pointsPerGame: number;
  reboundsPerGame: number;
  assistsPerGame: number;
  per: number;
  efficiency: number;
  form: string; // "EXCELLENT", "TRÈS BON", "BON", "MOYEN", "FAIBLE"
  fieldGoalPercentage: number;
  threePointPercentage: number;
  freeThrowPercentage: number;
}