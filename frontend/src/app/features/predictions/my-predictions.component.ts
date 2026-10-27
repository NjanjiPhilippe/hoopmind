import { DatePipe, PercentPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, resource, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Api } from '../../core/api/generated/api';
import { deleteMyPrediction } from '../../core/api/generated/fn/mes-predictions/delete-my-prediction';
import { listMyPredictions } from '../../core/api/generated/fn/mes-predictions/list-my-predictions';
import { PredictionHistoryEntry } from '../../core/api/generated/models/prediction-history-entry';
import { toProblem } from '../../core/http/problem';
import { NbaCatalog } from '../../core/nba/nba-catalog';
import { currentValue } from '../../core/util/resource';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { TeamLogoComponent } from '../../shared/components/team-logo/team-logo.component';
import { predictParams, summarize } from './prediction-summary';

const PAGE_SIZE = 20;

/**
 * Les prédictions de l'utilisateur connecté, et seulement les siennes (le serveur filtre par compte) : chaque match
 * demandé sur la page Prédire, avec le score réel et le verdict une fois le match joué.
 */
@Component({
  selector: 'app-my-predictions',
  imports: [DatePipe, PercentPipe, RouterLink, NavbarComponent, TeamLogoComponent],
  templateUrl: './my-predictions.component.html',
  providers: [NbaCatalog],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MyPredictionsComponent {
  private readonly api = inject(Api);
  protected readonly catalog = inject(NbaCatalog);
  protected readonly summarize = summarize;
  protected readonly predictParams = predictParams;

  protected readonly page = signal(0);
  protected readonly predictions = resource({
    params: () => ({ page: this.page(), size: PAGE_SIZE }),
    loader: ({ params }) => this.api.invoke(listMyPredictions, params),
  });
  protected readonly rows = computed(() => currentValue(this.predictions)?.content ?? []);
  protected readonly pageInfo = computed(() => currentValue(this.predictions)?.page);
  /** Bons pronostics parmi les matchs déjà joués de la page. */
  protected readonly score = computed(() => {
    const played = this.rows().filter(row => row.correct != null);
    return played.length ? { correct: played.filter(row => row.correct).length, played: played.length } : null;
  });

  protected readonly busyId = signal<number | null>(null);
  protected readonly error = signal<string | null>(null);

  async remove(entry: PredictionHistoryEntry): Promise<void> {
    this.busyId.set(entry.id);
    this.error.set(null);
    try {
      await this.api.invoke(deleteMyPrediction, { id: entry.id });
      this.predictions.reload();
    } catch (e) {
      this.error.set(toProblem(e).detail ?? 'Suppression impossible');
    } finally {
      this.busyId.set(null);
    }
  }
}
