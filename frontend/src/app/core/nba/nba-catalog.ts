import { Injectable, computed, inject, resource } from '@angular/core';

import { Api } from '../api/generated/api';
import { getAllSeasons } from '../api/generated/fn/seasons/get-all-seasons';
import { getAllTeams } from '../api/generated/fn/teams/get-all-teams';
import { SeasonResponse } from '../api/generated/models/season-response';
import { TeamResponse } from '../api/generated/models/team-response';
import { TeamSummaryResponse } from '../api/generated/models/team-summary-response';
import { currentValue } from '../util/resource';

/**
 * Saisons et équipes en base, partagées par les pages d'une même vue. Fourni au niveau de chaque page
 * ({@code providers: [NbaCatalog]}) : la liste est relue à chaque visite, donc à jour après un import.
 */
@Injectable()
export class NbaCatalog {
  private readonly api = inject(Api);

  readonly seasonsResource = resource({ loader: () => this.api.invoke(getAllSeasons) });
  readonly teamsResource = resource({ loader: () => this.api.invoke(getAllTeams) });

  /** De la plus récente à la plus ancienne. */
  readonly seasons = computed(() => [...(currentValue(this.seasonsResource) ?? [])].sort((a, b) => (b.year ?? 0) - (a.year ?? 0)));
  readonly latestSeason = computed<SeasonResponse | null>(() => this.seasons()[0] ?? null);
  readonly teams = computed(() => [...(currentValue(this.teamsResource) ?? [])].sort((a, b) => teamName(a).localeCompare(teamName(b), 'fr')));
  readonly teamsById = computed(() => new Map(this.teams().map(team => [team.id!, team])));
  readonly loading = computed(() => this.seasonsResource.isLoading() || this.teamsResource.isLoading());
  readonly failed = computed(() => !!this.seasonsResource.error() || !!this.teamsResource.error());
  /** Rien n'a encore été importé : les pages affichent un état vide qui oriente vers l'import. */
  readonly empty = computed(() => !this.loading() && !this.failed() && this.seasons().length === 0);

  season(id: number | null | undefined): SeasonResponse | null {
    return this.seasons().find(season => season.id === id) ?? null;
  }
}

/** « Boston Celtics ». */
export function teamName(team: TeamResponse | TeamSummaryResponse | null | undefined): string {
  return team ? [team.city, team.name].filter(Boolean).join(' ') : '';
}

/** Saison demandée dans l'URL si elle existe, sinon la plus récente. */
export function seasonOrLatest(catalog: NbaCatalog, requested: number | undefined): number | null {
  return catalog.season(requested)?.id ?? catalog.latestSeason()?.id ?? null;
}

/** Transforme un paramètre de requête en identifiant numérique (ou undefined). */
export function idParam(value: unknown): number | undefined {
  const id = typeof value === 'number' ? value : Number.parseInt(String(value ?? ''), 10);
  return Number.isInteger(id) && id > 0 ? id : undefined;
}
