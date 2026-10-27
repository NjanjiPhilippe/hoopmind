import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, linkedSignal, resource, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Api } from '../../../core/api/generated/api';
import { createPlayer } from '../../../core/api/generated/fn/players/create-player';
import { deletePlayer } from '../../../core/api/generated/fn/players/delete-player';
import { deletePlayerSeasonStats } from '../../../core/api/generated/fn/players/delete-player-season-stats';
import { getPlayerSeasonStats } from '../../../core/api/generated/fn/players/get-player-season-stats';
import { getPlayersByTeam } from '../../../core/api/generated/fn/players/get-players-by-team';
import { savePlayerSeasonStats } from '../../../core/api/generated/fn/players/save-player-season-stats';
import { updatePlayer } from '../../../core/api/generated/fn/players/update-player';
import { PlayerResponse } from '../../../core/api/generated/models/player-response';
import { PlayerSeasonStatsResponse } from '../../../core/api/generated/models/player-season-stats-response';
import { toProblem } from '../../../core/http/problem';
import { NbaCatalog, teamName } from '../../../core/nba/nba-catalog';
import { currentValue } from '../../../core/util/resource';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { TeamLogoComponent } from '../../../shared/components/team-logo/team-logo.component';
import { POSITIONS, STATUSES, STAT_FIELDS, parsePlayer, parseStats, positionLabel, statValue, statusLabel } from './player-entry';

/**
 * Saisie des joueurs par un administrateur : effectif de chaque équipe (ajout, modification, suppression) et
 * statistiques de saison de chaque joueur (moyennes par match, pourcentages).
 */
@Component({
  selector: 'app-admin-players',
  imports: [DecimalPipe, RouterLink, NavbarComponent, TeamLogoComponent],
  providers: [NbaCatalog],
  templateUrl: './admin-players.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminPlayersComponent {
  private readonly api = inject(Api);
  protected readonly catalog = inject(NbaCatalog);
  protected readonly currentValue = currentValue;
  protected readonly teamName = teamName;
  protected readonly positions = POSITIONS;
  protected readonly statuses = STATUSES;
  protected readonly statFields = STAT_FIELDS;
  protected readonly positionLabel = positionLabel;
  protected readonly statusLabel = statusLabel;
  protected readonly statValue = statValue;

  protected readonly teamId = linkedSignal(() => this.catalog.teams()[0]?.id ?? null);
  protected readonly team = computed(() => this.catalog.teamsById().get(this.teamId() ?? -1) ?? null);
  protected readonly roster = resource({
    params: () => this.teamId() ?? undefined,
    loader: ({ params }) => this.api.invoke(getPlayersByTeam, { teamId: params }),
  });
  protected readonly players = computed(() =>
    [...(currentValue(this.roster) ?? [])].sort((a, b) => (a.lastName ?? '').localeCompare(b.lastName ?? '', 'fr')));

  /** Joueur en cours de modification ; null : le formulaire ajoute un joueur. Réinitialisé au changement d'équipe. */
  protected readonly editing = linkedSignal<number | null, PlayerResponse | null>({ source: this.teamId, computation: () => null });
  /** Joueur dont on saisit les statistiques. */
  protected readonly selected = linkedSignal<number | null, PlayerResponse | null>({ source: this.teamId, computation: () => null });
  protected readonly stats = resource({
    params: () => this.selected()?.id,
    loader: ({ params }) => this.api.invoke(getPlayerSeasonStats, { id: params }),
  });
  protected readonly statsSeasonId = linkedSignal(() => this.catalog.latestSeason()?.id ?? null);
  /** Ligne déjà saisie pour la saison choisie : le formulaire la reprend. */
  protected readonly statsForSeason = computed<PlayerSeasonStatsResponse | null>(() =>
    (currentValue(this.stats) ?? []).find(row => row.season?.id === this.statsSeasonId()) ?? null);

  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly notice = signal<string | null>(null);

  async savePlayer(event: SubmitEvent): Promise<void> {
    event.preventDefault();
    const form = event.target as HTMLFormElement;
    const parsed = parsePlayer(formValues(form));
    const teamId = this.teamId();
    if ('error' in parsed) {
      return this.fail(parsed.error);
    }
    if (teamId == null) {
      return this.fail('Choisissez une équipe.');
    }
    const editing = this.editing();
    await this.run(async () => {
      if (editing) {
        await this.api.invoke(updatePlayer, { id: editing.id!, body: { ...parsed, teamId } });
      } else {
        const { status: _status, ...create } = parsed;
        await this.api.invoke(createPlayer, { body: { ...create, teamId } });
      }
      form.reset();
      this.editing.set(null);
      this.roster.reload();
    }, editing ? `${parsed.firstName} ${parsed.lastName} modifié.` : `${parsed.firstName} ${parsed.lastName} ajouté.`);
  }

  async removePlayer(player: PlayerResponse): Promise<void> {
    if (!confirm(`Supprimer ${player.firstName} ${player.lastName} et ses statistiques ?`)) {
      return;
    }
    await this.run(async () => {
      await this.api.invoke(deletePlayer, { id: player.id! });
      if (this.selected()?.id === player.id) {
        this.selected.set(null);
      }
      this.roster.reload();
    }, `${player.firstName} ${player.lastName} supprimé.`);
  }

  async saveStats(event: SubmitEvent): Promise<void> {
    event.preventDefault();
    const player = this.selected();
    const seasonId = this.statsSeasonId();
    const parsed = parseStats(formValues(event.target as HTMLFormElement));
    if ('error' in parsed) {
      return this.fail(parsed.error);
    }
    if (!player || seasonId == null) {
      return this.fail('Choisissez une saison.');
    }
    await this.run(async () => {
      await this.api.invoke(savePlayerSeasonStats, { id: player.id!, seasonId, body: parsed });
      this.stats.reload();
    }, `Statistiques de ${player.firstName} ${player.lastName} enregistrées (${this.catalog.season(seasonId)?.name}).`);
  }

  async removeStats(row: PlayerSeasonStatsResponse): Promise<void> {
    const player = this.selected();
    if (!player || !row.season?.id || !confirm(`Retirer les statistiques ${row.season.name} ?`)) {
      return;
    }
    await this.run(async () => {
      await this.api.invoke(deletePlayerSeasonStats, { id: player.id!, seasonId: row.season!.id! });
      this.stats.reload();
    }, `Statistiques ${row.season.name} retirées.`);
  }

  private fail(message: string): void {
    this.error.set(message);
    this.notice.set(null);
  }

  private async run(action: () => Promise<void>, notice: string): Promise<void> {
    this.busy.set(true);
    this.error.set(null);
    this.notice.set(null);
    try {
      await action();
      this.notice.set(notice);
    } catch (e) {
      this.error.set(toProblem(e).detail ?? 'Enregistrement impossible');
    } finally {
      this.busy.set(false);
    }
  }
}

function formValues(form: HTMLFormElement): Record<string, string> {
  return Object.fromEntries([...new FormData(form).entries()].map(([key, value]) => [key, String(value)]));
}
