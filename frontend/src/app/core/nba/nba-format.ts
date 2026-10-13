import { GameResponse } from '../api/generated/models/game-response';
import { SeasonResponse } from '../api/generated/models/season-response';

/** Date du jour au format yyyy-mm-dd, dans le fuseau du navigateur. */
export function today(now = new Date()): string {
  const local = new Date(now.getTime() - now.getTimezoneOffset() * 60_000);
  return local.toISOString().slice(0, 10);
}

/**
 * Jour proposé par défaut pour une prédiction : aujourd'hui pendant la saison, sinon le dernier jour connu de la saison
 * (la forme des équipes est calculée sur les matchs joués avant ce jour).
 */
export function defaultPredictionDate(season: SeasonResponse | null | undefined, now = new Date()): string {
  const day = today(now);
  if (!season?.startDate || !season.endDate) {
    return day;
  }
  if (day < season.startDate) {
    return season.startDate;
  }
  return day > season.endDate ? season.endDate : day;
}

const GAME_TYPE_LABELS: Partial<Record<NonNullable<GameResponse['gameType']>, string>> = {
  PLAY_IN: 'Play-in',
  PLAYOFFS: 'Playoffs',
  FINALS: 'Finales',
  PRESEASON: 'Présaison',
  ALL_STAR: 'All-Star',
};

/** Libellé du type de match, vide pour la saison régulière (le cas courant n'a pas besoin de badge). */
export function gameTypeLabel(type: GameResponse['gameType']): string {
  return type ? GAME_TYPE_LABELS[type] ?? '' : '';
}

export function isPlayoffGame(game: GameResponse): boolean {
  return game.gameType === 'PLAYOFFS' || game.gameType === 'FINALS';
}

/** Matchs du plus récent au plus ancien ; à date égale, l'ordre de l'API est conservé. */
export function byDateDesc(a: GameResponse, b: GameResponse): number {
  return (b.scheduledDate ?? '').localeCompare(a.scheduledDate ?? '');
}

/** « 3 V » ou « 2 D » : série en cours (positive pour des victoires, négative pour des défaites). */
export function streakLabel(streak: number | null | undefined): string {
  if (!streak) {
    return '';
  }
  return streak > 0 ? `${streak} V` : `${-streak} D`;
}
