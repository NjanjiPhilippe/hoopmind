import { GameResponse } from '../../../core/api/generated/models/game-response';

/** Matchs d'un jour (yyyy-mm-dd), dans l'ordre de l'heure prévue. */
export function gamesOn(games: GameResponse[], day: string): GameResponse[] {
  return games
    .filter(game => game.scheduledDate?.slice(0, 10) === day)
    .sort((a, b) => (a.scheduledDate ?? '').localeCompare(b.scheduledDate ?? '') || (a.id ?? 0) - (b.id ?? 0));
}

export type ScoreInput = { homeTeamScore: number; awayTeamScore: number; overtimePeriods: number } | { error: string };

/** Vérifie la saisie avant l'envoi : deux scores entiers, pas d'égalité (le serveur vérifie aussi). */
export function parseScore(home: string, away: string, overtime: string): ScoreInput {
  const homeTeamScore = Number(home);
  const awayTeamScore = Number(away);
  if (home.trim() === '' || away.trim() === '' || !Number.isInteger(homeTeamScore) || !Number.isInteger(awayTeamScore)) {
    return { error: 'Saisissez les deux scores.' };
  }
  if (homeTeamScore < 0 || awayTeamScore < 0 || homeTeamScore > 250 || awayTeamScore > 250) {
    return { error: 'Un score va de 0 à 250 points.' };
  }
  if (homeTeamScore === awayTeamScore) {
    return { error: 'Un match NBA ne se termine pas sur une égalité.' };
  }
  return { homeTeamScore, awayTeamScore, overtimePeriods: Number(overtime) || 0 };
}

/** Jour suivant ou précédent, au format yyyy-mm-dd. */
export function shiftDay(day: string, days: number): string {
  const date = new Date(`${day}T12:00:00Z`);
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
}
