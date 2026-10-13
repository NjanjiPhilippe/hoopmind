import { GameResponse } from '../../core/api/generated/models/game-response';
import { CalendarGame } from '../../core/nba/team-calendar';

/** Matchs d'un jour (« 2023-11-02 »), dans l'ordre de l'API. */
export function gamesOn(games: GameResponse[], day: string): GameResponse[] {
  return games.filter(game => game.scheduledDate?.slice(0, 10) === day);
}

/**
 * Match proposé quand on choisit une équipe : son prochain match à jouer, sinon le dernier de la saison.
 */
export function defaultGame(entries: CalendarGame[]): CalendarGame | undefined {
  return entries.find(entry => entry.next) ?? entries.at(-1);
}
