import { PredictionHistoryEntry } from '../../core/api/generated/models/prediction-history-entry';

export interface PredictionSummary {
  /** Équipe donnée gagnante et sa probabilité de victoire. */
  winner: string;
  probability: number;
  /** « 112-105 » (domicile-extérieur), ou null si le modèle de score n'était pas entraîné. */
  predictedScore: string | null;
  actualScore: string | null;
  verdict: 'correct' | 'wrong' | 'pending';
}

/** Ce qu'une ligne de « Mes prédictions » affiche : le favori, le score prédit, puis le verdict une fois le match joué. */
export function summarize(entry: PredictionHistoryEntry): PredictionSummary {
  const homeFavourite = entry.homeWinProbability >= 0.5;
  return {
    winner: homeFavourite ? entry.homeTeam : entry.awayTeam,
    probability: homeFavourite ? entry.homeWinProbability : 1 - entry.homeWinProbability,
    predictedScore: score(entry.predictedHomePoints, entry.predictedAwayPoints),
    actualScore: score(entry.actualHomePoints, entry.actualAwayPoints),
    verdict: entry.correct == null ? 'pending' : entry.correct ? 'correct' : 'wrong',
  };
}

/** Paramètres de la page Prédire pour revoir (et recalculer) cette prédiction. */
export function predictParams(entry: PredictionHistoryEntry): Record<string, string | number | boolean> {
  return { season: entry.seasonId, home: entry.homeTeamId, away: entry.awayTeamId, date: entry.gameDate, playoff: entry.playoff };
}

function score(home: number | undefined, away: number | undefined): string | null {
  return home != null && away != null ? `${home}-${away}` : null;
}
