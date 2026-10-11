import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, linkedSignal, resource } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { Api } from '../../core/api/generated/api';
import { getGamesBySeason } from '../../core/api/generated/fn/games/get-games-by-season';
import { GameResponse } from '../../core/api/generated/models/game-response';
import { AuthService } from '../../core/auth/auth.service';
import { NbaCatalog, idParam, seasonOrLatest, teamName } from '../../core/nba/nba-catalog';
import { byDateDesc, gameTypeLabel, isPlayoffGame } from '../../core/nba/nba-format';
import { currentValue } from '../../core/util/resource';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';

const PAGE_SIZE = 50;

/** Matchs d'une saison, filtrés sur une équipe si besoin, du plus récent au plus ancien. */
export function filterGames(games: GameResponse[], teamId: number | null): GameResponse[] {
  return games
    .filter(game => teamId === null || game.homeTeam?.id === teamId || game.awayTeam?.id === teamId)
    .sort(byDateDesc);
}

@Component({
  selector: 'app-games',
  imports: [DatePipe, RouterLink, NavbarComponent],
  templateUrl: './games.component.html',
  providers: [NbaCatalog],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GamesComponent {
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthService);
  protected readonly catalog = inject(NbaCatalog);
  protected readonly teamName = teamName;
  protected readonly gameTypeLabel = gameTypeLabel;

  readonly season = input(undefined, { transform: idParam });
  readonly team = input(undefined, { transform: idParam });

  protected readonly seasonId = linkedSignal(() => seasonOrLatest(this.catalog, this.season()));
  protected readonly teamId = linkedSignal<number | null>(() => this.team() ?? null);

  protected readonly games = resource({
    params: () => this.seasonId() ?? undefined,
    loader: ({ params }) => this.api.invoke(getGamesBySeason, { seasonId: params }),
  });
  protected readonly filtered = computed(() => filterGames(currentValue(this.games) ?? [], this.teamId()));
  /** Une saison complète compte plus de 1 200 matchs : ils s'affichent par tranches. */
  protected readonly shown = linkedSignal({ source: () => [this.seasonId(), this.teamId()], computation: () => PAGE_SIZE });
  protected readonly visible = computed(() => this.filtered().slice(0, this.shown()));

  onFilter(change: { season?: string; team?: string }): void {
    if (change.season !== undefined) {
      this.seasonId.set(idParam(change.season) ?? null);
    }
    if (change.team !== undefined) {
      this.teamId.set(idParam(change.team) ?? null);
    }
    void this.router.navigate([], { queryParams: { season: this.seasonId(), team: this.teamId() }, replaceUrl: true });
  }

  showMore(): void {
    this.shown.update(count => count + PAGE_SIZE);
  }

  /** Lien vers la prédiction de ce match, à la date du match. */
  predictParams(game: GameResponse) {
    return {
      season: game.season?.id,
      home: game.homeTeam?.id,
      away: game.awayTeam?.id,
      date: game.scheduledDate?.slice(0, 10),
      playoff: isPlayoffGame(game) || null,
    };
  }

  won(game: GameResponse, side: 'home' | 'away'): boolean {
    const team = side === 'home' ? game.homeTeam : game.awayTeam;
    return !!game.result?.winner && game.result.winner.id === team?.id;
  }
}
