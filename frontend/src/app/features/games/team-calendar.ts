import { GameResponse } from '../../core/api/generated/models/game-response';
import { TeamSummaryResponse } from '../../core/api/generated/models/team-summary-response';

/** Un match vu par l'équipe choisie : adversaire, lieu, et résultat une fois joué. */
export interface CalendarGame {
  game: GameResponse;
  home: boolean;
  opponent: TeamSummaryResponse | undefined;
  teamScore: number | null;
  opponentScore: number | null;
  /** « V » ou « D » une fois le match joué, sinon null. */
  outcome: 'V' | 'D' | null;
  /** Premier match pas encore joué de la saison. */
  next: boolean;
}

export interface CalendarMonth {
  /** « 2023-10 » : sert de clé et, avec DatePipe, de titre (« octobre 2023 »). */
  month: string;
  games: CalendarGame[];
}

export interface TeamCalendar {
  months: CalendarMonth[];
  wins: number;
  losses: number;
}

/**
 * Calendrier d'une équipe sur une saison, du premier au dernier match, par mois. Le bilan compte tous les matchs
 * joués (saison régulière, play-in et playoffs) : c'est celui que l'on lit en parcourant le calendrier.
 */
export function teamCalendar(games: GameResponse[], teamId: number): TeamCalendar {
  const sorted = games
    .filter(game => game.homeTeam?.id === teamId || game.awayTeam?.id === teamId)
    .sort((a, b) => (a.scheduledDate ?? '').localeCompare(b.scheduledDate ?? '') || (a.id ?? 0) - (b.id ?? 0));

  const months: CalendarMonth[] = [];
  let wins = 0;
  let losses = 0;
  let nextFound = false;
  for (const game of sorted) {
    const home = game.homeTeam?.id === teamId;
    const result = game.result;
    const teamScore = result ? (home ? result.homeTeamScore : result.awayTeamScore) ?? null : null;
    const opponentScore = result ? (home ? result.awayTeamScore : result.homeTeamScore) ?? null : null;
    const outcome: CalendarGame['outcome'] = teamScore === null || opponentScore === null ? null : teamScore > opponentScore ? 'V' : 'D';
    if (outcome === 'V') wins++;
    if (outcome === 'D') losses++;
    const next: boolean = outcome === null && !nextFound;
    nextFound ||= next;

    const month = (game.scheduledDate ?? '').slice(0, 7);
    if (months.at(-1)?.month !== month) {
      months.push({ month, games: [] });
    }
    months.at(-1)!.games.push({
      game, home, opponent: home ? game.awayTeam : game.homeTeam, teamScore, opponentScore, outcome, next,
    });
  }
  return { months, wins, losses };
}
