import { ChangeDetectionStrategy, Component, computed, inject, linkedSignal, resource, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Api } from '../../../core/api/generated/api';
import { createGame } from '../../../core/api/generated/fn/games/create-game';
import { deleteGameResult } from '../../../core/api/generated/fn/games/delete-game-result';
import { getGamesBySeason } from '../../../core/api/generated/fn/games/get-games-by-season';
import { recordGameResult } from '../../../core/api/generated/fn/games/record-game-result';
import { GameResponse } from '../../../core/api/generated/models/game-response';
import { toProblem } from '../../../core/http/problem';
import { NbaCatalog, teamName } from '../../../core/nba/nba-catalog';
import { defaultPredictionDate } from '../../../core/nba/nba-format';
import { currentValue } from '../../../core/util/resource';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { TeamLogoComponent } from '../../../shared/components/team-logo/team-logo.component';
import { gamesOn, parseScore, shiftDay } from './results-entry';

/**
 * Saisie des scores de la saison en cours par un administrateur, quand l'import n'est pas disponible. Le serveur ne
 * stocke que les scores : la forme des équipes (victoires, 10 derniers matchs) est recalculée à chaque prédiction.
 */
@Component({
  selector: 'app-admin-results',
  imports: [RouterLink, NavbarComponent, TeamLogoComponent],
  providers: [NbaCatalog],
  templateUrl: './admin-results.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminResultsComponent {
  private readonly api = inject(Api);
  protected readonly catalog = inject(NbaCatalog);
  protected readonly teamName = teamName;

  protected readonly seasonId = linkedSignal(() => this.catalog.latestSeason()?.id ?? null);
  protected readonly day = linkedSignal(() => defaultPredictionDate(this.catalog.season(this.seasonId())));

  protected readonly games = resource({
    params: () => this.seasonId() ?? undefined,
    loader: ({ params }) => this.api.invoke(getGamesBySeason, { seasonId: params }),
  });
  protected readonly dayGames = computed(() => gamesOn(currentValue(this.games) ?? [], this.day()));

  protected readonly busyGameId = signal<number | null>(null);
  protected readonly adding = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly notice = signal<string | null>(null);

  previousDay(): void {
    this.day.update(day => shiftDay(day, -1));
  }

  nextDay(): void {
    this.day.update(day => shiftDay(day, 1));
  }

  save(game: GameResponse, home: string, away: string, overtime: string): Promise<void> {
    const score = parseScore(home, away, overtime);
    if ('error' in score) {
      this.error.set(score.error);
      this.notice.set(null);
      return Promise.resolve();
    }
    return this.run(game.id!, () => this.api.invoke(recordGameResult, { id: game.id!, body: score }),
      `Score enregistré : ${game.homeTeam?.abbreviation} ${score.homeTeamScore} - ${score.awayTeamScore} ${game.awayTeam?.abbreviation}.`);
  }

  clear(game: GameResponse): Promise<void> {
    if (!confirm('Retirer ce score ? Le match redeviendra « à venir ».')) {
      return Promise.resolve();
    }
    return this.run(game.id!, () => this.api.invoke(deleteGameResult, { id: game.id! }), 'Score retiré.');
  }

  async add(homeTeamId: string, awayTeamId: string, time: string): Promise<void> {
    const seasonId = this.seasonId();
    if (!seasonId || !homeTeamId || !awayTeamId) {
      this.error.set('Choisissez les deux équipes.');
      return;
    }
    if (homeTeamId === awayTeamId) {
      this.error.set('Une équipe ne peut pas jouer contre elle-même.');
      return;
    }
    this.adding.set(true);
    this.error.set(null);
    this.notice.set(null);
    try {
      await this.api.invoke(createGame, {
        body: {
          seasonId,
          homeTeamId: Number(homeTeamId),
          awayTeamId: Number(awayTeamId),
          gameType: 'REGULAR_SEASON',
          scheduledDate: `${this.day()}T${time || '19:00'}:00`,
        },
      });
      this.notice.set('Match ajouté : saisissez son score dès qu’il est joué.');
      this.games.reload();
    } catch (e) {
      this.error.set(toProblem(e).detail ?? 'Le match n’a pas pu être ajouté.');
    } finally {
      this.adding.set(false);
    }
  }

  private async run(gameId: number, action: () => Promise<unknown>, success: string): Promise<void> {
    this.busyGameId.set(gameId);
    this.error.set(null);
    this.notice.set(null);
    try {
      await action();
      this.notice.set(success);
      this.games.reload();
    } catch (e) {
      this.error.set(toProblem(e).detail ?? 'L’enregistrement a échoué.');
    } finally {
      this.busyGameId.set(null);
    }
  }
}
