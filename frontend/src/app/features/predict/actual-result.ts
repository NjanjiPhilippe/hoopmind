import { GameResponse } from '../../core/api/generated/models/game-response';

/** Résultat réel d'un match prédit, et si le favori du modèle l'a emporté. */
export interface ActualResult {
  homeScore: number;
  awayScore: number;
  homeWon: boolean;
  overtime: boolean;
  /** Le favori du modèle (probabilité de victoire à domicile ≥ 50 %) a gagné. */
  correct: boolean;
}

/**
 * Le résultat du match si la prédiction porte bien sur lui (mêmes équipes, même côté) et qu'il a été joué ; sinon null.
 */
export function actualResult(game: GameResponse | null | undefined, homeTeamId: number, awayTeamId: number,
                             homeWinProbability: number): ActualResult | null {
  const result = game?.result;
  if (!result || result.homeTeamScore == null || result.awayTeamScore == null
      || game.homeTeam?.id !== homeTeamId || game.awayTeam?.id !== awayTeamId) {
    return null;
  }
  const homeWon = result.homeTeamScore > result.awayTeamScore;
  return {
    homeScore: result.homeTeamScore,
    awayScore: result.awayTeamScore,
    homeWon,
    overtime: !!result.overtime,
    correct: homeWon === homeWinProbability >= 0.5,
  };
}
