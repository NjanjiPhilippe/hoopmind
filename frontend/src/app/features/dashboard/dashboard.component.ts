import { DatePipe, DecimalPipe, PercentPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, resource } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Api } from '../../core/api/generated/api';
import { getGamesBySeason } from '../../core/api/generated/fn/games/get-games-by-season';
import { getModelMetrics } from '../../core/api/generated/fn/spark-analytics/get-model-metrics';
import { GameResponse } from '../../core/api/generated/models/game-response';
import { TrainingReport } from '../../core/api/generated/models/training-report';
import { AuthService } from '../../core/auth/auth.service';
import { NbaCatalog, teamName } from '../../core/nba/nba-catalog';
import { byDateDesc } from '../../core/nba/nba-format';
import { currentValue } from '../../core/util/resource';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';

const LATEST = 5;

/** Derniers résultats et prochains matchs d'une saison. */
export function splitGames(games: GameResponse[], count = LATEST): { results: GameResponse[]; upcoming: GameResponse[] } {
  const sorted = [...games].sort(byDateDesc);
  return {
    results: sorted.filter(game => game.result).slice(0, count),
    upcoming: sorted.filter(game => !game.result && game.status === 'SCHEDULED').reverse().slice(0, count),
  };
}

/**
 * Première page après la connexion : ce que l'application sait faire (prédire), la qualité du modèle, et les derniers
 * matchs de la saison. Sans données, elle oriente vers l'import (administrateur) ou explique l'attente (utilisateur).
 */
@Component({
  selector: 'app-dashboard',
  imports: [DatePipe, DecimalPipe, PercentPipe, RouterLink, NavbarComponent],
  templateUrl: './dashboard.component.html',
  providers: [NbaCatalog],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent {
  protected readonly currentValue = currentValue;
  private readonly api = inject(Api);
  protected readonly auth = inject(AuthService);
  protected readonly catalog = inject(NbaCatalog);
  protected readonly teamName = teamName;

  protected readonly firstName = computed(() => this.auth.user()?.firstName || this.auth.user()?.username);

  protected readonly model = resource({ loader: () => this.latestModel() });
  protected readonly games = resource({
    params: () => this.catalog.latestSeason()?.id,
    loader: ({ params }) => this.api.invoke(getGamesBySeason, { seasonId: params }),
  });
  protected readonly split = computed(() => splitGames(currentValue(this.games) ?? []));
  protected readonly modelGain = computed(() => {
    const report = currentValue(this.model);
    return report?.accuracy != null && report.betterRecordBaseline != null ? report.accuracy - report.betterRecordBaseline : null;
  });

  private async latestModel(): Promise<TrainingReport | null> {
    try {
      return await this.api.invoke(getModelMetrics);
    } catch (e) {
      if (e instanceof HttpErrorResponse && (e.status === 404 || e.status === 503)) {
        return null;
      }
      throw e;
    }
  }
}
