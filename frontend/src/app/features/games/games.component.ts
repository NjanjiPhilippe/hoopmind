import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, linkedSignal, resource } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { Api } from '../../core/api/generated/api';
import { getGamesBySeason } from '../../core/api/generated/fn/games/get-games-by-season';
import { GameResponse } from '../../core/api/generated/models/game-response';
import { AuthService } from '../../core/auth/auth.service';
import { NbaCatalog, idParam, seasonOrLatest, teamName } from '../../core/nba/nba-catalog';
import { defaultPredictionDate, gameTypeLabel, isPlayoffGame } from '../../core/nba/nba-format';
import { teamColor } from '../../core/nba/team-branding';
import { currentValue } from '../../core/util/resource';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { MonthCalendarComponent } from '../../shared/components/month-calendar/month-calendar.component';
import { monthOf } from '../../shared/components/month-calendar/month-grid';
import { TeamLogoComponent } from '../../shared/components/team-logo/team-logo.component';
import { teamCalendar } from '../../core/nba/team-calendar';

@Component({
  selector: 'app-games',
  imports: [DatePipe, RouterLink, NavbarComponent, MonthCalendarComponent, TeamLogoComponent],
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
  protected readonly teamColor = teamColor;

  readonly season = input(undefined, { transform: idParam });
  readonly team = input(undefined, { transform: idParam });

  protected readonly seasonId = linkedSignal(() => seasonOrLatest(this.catalog, this.season()));
  protected readonly teamId = linkedSignal<number | null>(() => this.team() ?? null);

  protected readonly games = resource({
    params: () => this.seasonId() ?? undefined,
    loader: ({ params }) => this.api.invoke(getGamesBySeason, { seasonId: params }),
  });
  /** Équipe choisie : son calendrier de la saison, mois par mois, avec son bilan. */
  protected readonly selectedTeam = computed(() => {
    const id = this.teamId();
    return id === null ? undefined : this.catalog.teamsById().get(id);
  });
  protected readonly calendar = computed(() => {
    const id = this.teamId();
    return id === null ? null : teamCalendar(currentValue(this.games) ?? [], id);
  });
  protected readonly allGames = computed(() => currentValue(this.games) ?? []);
  /** Jour choisi dans l'agenda (sans équipe) : par défaut aujourd'hui pendant la saison, sinon son dernier jour. */
  protected readonly day = linkedSignal(() => defaultPredictionDate(this.catalog.season(this.seasonId())));
  protected readonly dayGames = computed(() => this.allGames().filter(game => game.scheduledDate?.slice(0, 10) === this.day()));
  /** Mois affiché : celui du prochain match de l'équipe choisie, sinon celui du jour choisi. */
  protected readonly month = linkedSignal(() => {
    const next = this.calendar()?.months.flatMap(month => month.games).find(entry => entry.next);
    return next?.game.scheduledDate ? monthOf(next.game.scheduledDate) : monthOf(this.day());
  });
  /** Matchs de l'équipe dans le mois affiché, en liste sous l'agenda. */
  protected readonly monthEntries = computed(() => this.calendar()?.months.find(month => month.month === this.month())?.games ?? []);

  onFilter(change: { season?: string; team?: string }): void {
    if (change.season !== undefined) {
      this.seasonId.set(idParam(change.season) ?? null);
    }
    if (change.team !== undefined) {
      this.teamId.set(idParam(change.team) ?? null);
    }
    void this.router.navigate([], { queryParams: { season: this.seasonId(), team: this.teamId() }, replaceUrl: true });
  }

  /** Un match de l'agenda ouvre sa prédiction. */
  openPrediction(game: GameResponse): void {
    void this.router.navigate(['/predict'], { queryParams: this.predictParams(game) });
  }

  /** Lien vers la prédiction de ce match, à la date du match ; la page Prédire montre aussi son résultat réel. */
  predictParams(game: GameResponse) {
    return {
      game: game.id,
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
