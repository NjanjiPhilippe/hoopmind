import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, linkedSignal, resource } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { Api } from '../../core/api/generated/api';
import { getStandingsBySeason } from '../../core/api/generated/fn/statistics/get-standings-by-season';
import { TeamResponse } from '../../core/api/generated/models/team-response';
import { TeamStatsResponse } from '../../core/api/generated/models/team-stats-response';
import { AuthService } from '../../core/auth/auth.service';
import { NbaCatalog, idParam, seasonOrLatest, teamName } from '../../core/nba/nba-catalog';
import { streakLabel } from '../../core/nba/nba-format';
import { currentValue } from '../../core/util/resource';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';

export interface ConferenceStandings {
  conference: NonNullable<TeamResponse['conference']>;
  label: string;
  rows: TeamStatsResponse[];
}

/** Bilans d'une saison regroupés par conférence, du meilleur pourcentage de victoires au moins bon. */
export function byConference(stats: TeamStatsResponse[], teams: Map<number, TeamResponse>): ConferenceStandings[] {
  const sorted = [...stats].sort((a, b) => (b.winPercentage ?? 0) - (a.winPercentage ?? 0) || (b.wins ?? 0) - (a.wins ?? 0));
  return ([['EASTERN', 'Conférence Est'], ['WESTERN', 'Conférence Ouest']] as const)
    .map(([conference, label]) => ({ conference, label, rows: sorted.filter(row => teams.get(row.team?.id ?? -1)?.conference === conference) }))
    .filter(group => group.rows.length > 0);
}

@Component({
  selector: 'app-standings',
  imports: [DecimalPipe, RouterLink, NavbarComponent],
  templateUrl: './standings.component.html',
  providers: [NbaCatalog],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StandingsComponent {
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthService);
  protected readonly catalog = inject(NbaCatalog);
  protected readonly teamName = teamName;
  protected readonly streakLabel = streakLabel;

  readonly season = input(undefined, { transform: idParam });
  protected readonly seasonId = linkedSignal(() => seasonOrLatest(this.catalog, this.season()));

  protected readonly standings = resource({
    params: () => this.seasonId() ?? undefined,
    loader: ({ params }) => this.api.invoke(getStandingsBySeason, { seasonId: params }),
  });
  protected readonly conferences = computed(() => byConference(currentValue(this.standings) ?? [], this.catalog.teamsById()));

  onSeason(value: string): void {
    const id = idParam(value) ?? null;
    this.seasonId.set(id);
    void this.router.navigate([], { queryParams: { season: id }, replaceUrl: true });
  }
}
