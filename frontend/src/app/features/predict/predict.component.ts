import { DecimalPipe, PercentPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, effect, inject, input, linkedSignal, resource, signal, untracked } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { Api } from '../../core/api/generated/api';
import { getModelMetrics } from '../../core/api/generated/fn/spark-analytics/get-model-metrics';
import { predictGameOutcome } from '../../core/api/generated/fn/spark-analytics/predict-game-outcome';
import { GameOutcomePrediction } from '../../core/api/generated/models/game-outcome-prediction';
import { TrainingReport } from '../../core/api/generated/models/training-report';
import { AuthService } from '../../core/auth/auth.service';
import { toProblem } from '../../core/http/problem';
import { NbaCatalog, idParam, seasonOrLatest, teamName } from '../../core/nba/nba-catalog';
import { defaultPredictionDate } from '../../core/nba/nba-format';
import { currentValue } from '../../core/util/resource';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';

interface Prediction {
  homeTeamId: number;
  awayTeamId: number;
  date: string;
  playoff: boolean;
  result: GameOutcomePrediction;
}

/**
 * Prédire le vainqueur d'un match : deux équipes, une date, et le modèle calcule la forme de chacune avant ce jour.
 * Les paramètres de la page sont dans l'URL (lien « Prédire » de la liste des matchs, partage d'une prédiction).
 */
@Component({
  selector: 'app-predict',
  imports: [DecimalPipe, PercentPipe, RouterLink, NavbarComponent],
  templateUrl: './predict.component.html',
  providers: [NbaCatalog],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PredictComponent {
  protected readonly currentValue = currentValue;
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthService);
  protected readonly catalog = inject(NbaCatalog);
  protected readonly teamName = teamName;

  readonly season = input(undefined, { transform: idParam });
  readonly home = input(undefined, { transform: idParam });
  readonly away = input(undefined, { transform: idParam });
  readonly date = input<string>();
  readonly playoff = input(false, { transform: (value: unknown) => value === true || value === 'true' });

  protected readonly seasonId = linkedSignal(() => seasonOrLatest(this.catalog, this.season()));
  protected readonly homeTeamId = linkedSignal<number | null>(() => this.home() ?? null);
  protected readonly awayTeamId = linkedSignal<number | null>(() => this.away() ?? null);
  protected readonly day = linkedSignal(() => this.date() || defaultPredictionDate(this.catalog.season(this.seasonId())));
  protected readonly isPlayoff = linkedSignal(() => this.playoff());

  protected readonly sameTeam = computed(() => this.homeTeamId() !== null && this.homeTeamId() === this.awayTeamId());
  protected readonly canPredict = computed(() =>
    this.seasonId() !== null && this.homeTeamId() !== null && this.awayTeamId() !== null && !this.sameTeam() && !!this.day());

  protected readonly predicting = signal(false);
  protected readonly prediction = signal<Prediction | null>(null);
  protected readonly error = signal<{ text: string; notTrained: boolean } | null>(null);

  /** Qualité du modèle, montrée à côté de chaque prédiction pour savoir quelle confiance lui accorder. */
  protected readonly model = resource({ loader: () => this.latestModel() });

  constructor() {
    // Arrivée par un lien complet (depuis la liste des matchs) : la prédiction est faite dès que les saisons sont connues.
    const auto = effect(() => {
      if (this.home() && this.away() && !this.catalog.loading()) {
        auto.destroy();
        untracked(() => void this.predict());
      }
    });
  }

  team(id: number | null) {
    return id === null ? undefined : this.catalog.teamsById().get(id);
  }

  onSeason(value: string): void {
    this.seasonId.set(idParam(value) ?? null);
    this.day.set(defaultPredictionDate(this.catalog.season(this.seasonId())));
    this.prediction.set(null);
  }

  onTeam(side: 'home' | 'away', value: string): void {
    (side === 'home' ? this.homeTeamId : this.awayTeamId).set(idParam(value) ?? null);
    this.prediction.set(null);
  }

  swap(): void {
    const home = this.homeTeamId();
    this.homeTeamId.set(this.awayTeamId());
    this.awayTeamId.set(home);
    this.prediction.set(null);
  }

  async predict(): Promise<void> {
    if (!this.canPredict()) {
      return;
    }
    const request = {
      seasonId: this.seasonId()!,
      homeTeamId: this.homeTeamId()!,
      awayTeamId: this.awayTeamId()!,
      date: this.day(),
      playoff: this.isPlayoff(),
    };
    this.predicting.set(true);
    this.error.set(null);
    try {
      const result = await this.api.invoke(predictGameOutcome, request);
      this.prediction.set({ homeTeamId: request.homeTeamId, awayTeamId: request.awayTeamId, date: request.date, playoff: request.playoff, result });
      // L'URL reflète la prédiction affichée : elle se partage et survit au rechargement.
      void this.router.navigate([], {
        queryParams: { season: request.seasonId, home: request.homeTeamId, away: request.awayTeamId, date: request.date, playoff: request.playoff || null },
        replaceUrl: true,
      });
    } catch (e) {
      const problem = toProblem(e);
      const notTrained = problem.code === 'MODEL_NOT_TRAINED';
      this.prediction.set(null);
      this.error.set({
        text: notTrained ? 'Le modèle n\'a pas encore été entraîné : aucune prédiction n\'est possible pour l\'instant.' : problem.detail ?? 'Prédiction impossible',
        notTrained,
      });
    } finally {
      this.predicting.set(false);
    }
  }

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
