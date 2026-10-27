import { GameOutcomePrediction } from '../../core/api/generated/models/game-outcome-prediction';

/** Le score prédit prêt à afficher. */
export interface PredictedScore {
  homePoints: number;
  awayPoints: number;
  homeLeads: boolean;
  /** Écart arrondi, au moins 1 point. */
  margin: number;
  total: number;
  /** Erreur moyenne du modèle sur l'écart, arrondie. */
  marginError: number;
  /** Écart avant arrondi, du point de vue de l'équipe à domicile : sert à le comparer au résultat réel. */
  rawMargin: number;
  /** L'équipe qui mène dans le score est aussi le favori du modèle du vainqueur. */
  agreesWithFavorite: boolean;
  homePointsPerGame: number;
  homePointsAllowedPerGame: number;
  awayPointsPerGame: number;
  awayPointsAllowedPerGame: number;
}

/** Null tant que le modèle n'a pas été réentraîné depuis l'ajout du modèle de score. */
export function predictedScore(prediction: GameOutcomePrediction | undefined): PredictedScore | null {
  const score = prediction?.score;
  if (!score || score.homePoints == null || score.awayPoints == null) {
    return null;
  }
  const homeLeads = score.homePoints > score.awayPoints;
  return {
    homePoints: score.homePoints,
    awayPoints: score.awayPoints,
    homeLeads,
    margin: Math.max(1, Math.round(Math.abs(score.margin ?? score.homePoints - score.awayPoints))),
    total: Math.round(score.total ?? score.homePoints + score.awayPoints),
    marginError: Math.round(score.marginError ?? 0),
    rawMargin: score.margin ?? score.homePoints - score.awayPoints,
    agreesWithFavorite: homeLeads === (prediction?.homeWinProbability ?? 0.5) >= 0.5,
    homePointsPerGame: score.homePointsPerGame ?? 0,
    homePointsAllowedPerGame: score.homePointsAllowedPerGame ?? 0,
    awayPointsPerGame: score.awayPointsPerGame ?? 0,
    awayPointsAllowedPerGame: score.awayPointsAllowedPerGame ?? 0,
  };
}

/** De combien de points l'écart prédit s'est trompé sur l'écart réel (arrondi). */
export function marginMiss(score: PredictedScore, homeScore: number, awayScore: number): number {
  return Math.round(Math.abs(score.rawMargin - (homeScore - awayScore)));
}

/** « 1 point », « 7 points ». */
export function points(value: number): string {
  return `${value} point${Math.abs(value) > 1 ? 's' : ''}`;
}
