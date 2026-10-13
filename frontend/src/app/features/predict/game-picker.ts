import { GameResponse } from '../../core/api/generated/models/game-response';
import { teamName } from '../../core/nba/nba-catalog';
import { CalendarGame } from '../games/team-calendar';

/** Matchs d'un jour (« 2023-11-02 »), dans l'ordre de l'API. */
export function gamesOn(games: GameResponse[], day: string): GameResponse[] {
  return games.filter(game => game.scheduledDate?.slice(0, 10) === day);
}

/** « 2 nov. · @ Los Angeles Clippers · V 115-112 », pour la liste des matchs d'une équipe. */
export function calendarLabel(entry: CalendarGame, date: (value: string) => string): string {
  const parts = [date(entry.game.scheduledDate ?? ''), `${entry.home ? 'vs' : '@'} ${teamName(entry.opponent)}`];
  if (entry.outcome) {
    parts.push(`${entry.outcome} ${entry.teamScore}-${entry.opponentScore}`);
  } else if (entry.next) {
    parts.push('prochain match');
  }
  return parts.join(' · ');
}

/**
 * Match proposé quand on choisit une équipe : son prochain match à jouer, sinon le dernier de la saison.
 */
export function defaultGame(entries: CalendarGame[]): CalendarGame | undefined {
  return entries.find(entry => entry.next) ?? entries.at(-1);
}
