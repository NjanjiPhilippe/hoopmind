import { DatePipe, DecimalPipe, PercentPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, effect, inject, input, linkedSignal, resource, signal, untracked } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { Api } from '../../core/api/generated/api';
import { getModelMetrics } from '../../core/api/generated/fn/spark-analytics/get-model-metrics';
import { explainGameOutcome } from '../../core/api/generated/fn/spark-analytics/explain-game-outcome';
import { getGameById } from '../../core/api/generated/fn/games/get-game-by-id';
import { getGamesBySeason } from '../../core/api/generated/fn/games/get-games-by-season';
import { GameResponse } from '../../core/api/generated/models/game-response';
import { ExplainedPrediction } from '../../core/api/generated/models/explained-prediction';
import { TrainingReport } from '../../core/api/generated/models/training-report';
import { AuthService } from '../../core/auth/auth.service';
import { toProblem } from '../../core/http/problem';
import { NbaCatalog, idParam, seasonOrLatest, teamName } from '../../core/nba/nba-catalog';
import { defaultPredictionDate, isPlayoffGame } from '../../core/nba/nba-format';
import { matchupColors, textOn } from '../../core/nba/team-branding';
import { currentValue } from '../../core/util/resource';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { MonthCalendarComponent } from '../../shared/components/month-calendar/month-calendar.component';
import { monthOf } from '../../shared/components/month-calendar/month-grid';
import { TeamLogoComponent } from '../../shared/components/team-logo/team-logo.component';
import { teamCalendar } from '../../core/nba/team-calendar';
import { actualResult } from './actual-result';
import { defaultGame, gamesOn } from './game-picker';
import { FactorRow, explanationSource, factorRows } from './prediction-factors';

interface Prediction {
  homeTeamId: number;
  awayTeamId: number;
  date: string;
  playoff: boolean;
  result: ExplainedPrediction;
  factors: FactorRow[];
  source: string;
}

/**
 * Prédire le vainqueur d'un match : deux équipes, une date, et le modèle calcule la forme de chacune avant ce jour.
 * La prédiction arrive avec le poids de chaque facteur et une explication en français (rédigée par un LLM si le
 * serveur en a un, sinon automatiquement).
 * Les paramètres de la page sont dans l'URL (lien « Prédire » de la liste des matchs, partage d'une prédiction).
 */
@Component({
  selector: 'app-predict',
  imports: [DatePipe, DecimalPipe, PercentPipe, RouterLink, NavbarComponent, TeamLogoComponent, MonthCalendarComponent],
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
  protected readonly matchupColors = matchupColors;
  protected readonly textOn = textOn;
  protected readonly actualResult = actualResult;

  readonly season = input(undefined, { transform: idParam });
  readonly home = input(undefined, { transform: idParam });
  readonly away = input(undefined, { transform: idParam });
  readonly date = input<string>();
  /** Match du calendrier d'où vient la prédiction : son résultat réel est montré à côté. */
  readonly game = input(undefined, { transform: idParam });
  readonly playoff = input(false, { transform: (value: unknown) => value === true || value === 'true' });

  protected readonly seasonId = linkedSignal(() => seasonOrLatest(this.catalog, this.season()));
  protected readonly homeTeamId = linkedSignal<number | null>(() => this.home() ?? null);
  protected readonly awayTeamId = linkedSignal<number | null>(() => this.away() ?? null);
  protected readonly day = linkedSignal(() => this.date() || defaultPredictionDate(this.catalog.season(this.seasonId())));
  protected readonly isPlayoff = linkedSignal(() => this.playoff());
  /** Oublié dès que l'on change une équipe, la saison ou le jour : la prédiction ne porte plus sur ce match. */
  protected readonly gameId = linkedSignal<number | null>(() => this.game() ?? null);
  protected readonly actualGame = resource({
    params: () => this.gameId() ?? undefined,
    loader: ({ params }) => this.api.invoke(getGameById, { id: params }),
  });

  protected readonly sameTeam = computed(() => this.homeTeamId() !== null && this.homeTeamId() === this.awayTeamId());
  protected readonly canPredict = computed(() =>
    this.seasonId() !== null && this.homeTeamId() !== null && this.awayTeamId() !== null && !this.sameTeam() && !!this.day());

  protected readonly predicting = signal(false);
  protected readonly prediction = signal<Prediction | null>(null);
  protected readonly error = signal<{ text: string; notTrained: boolean } | null>(null);

  // Choix d'un match du calendrier en base : par équipe (sa saison) ou par jour (tous les matchs du jour).
  protected readonly seasonGames = resource({
    params: () => this.seasonId() ?? undefined,
    loader: ({ params }) => this.api.invoke(getGamesBySeason, { seasonId: params }),
  });
  protected readonly pickTeamId = signal<number | null>(null);
  protected readonly pickDay = linkedSignal(() => this.day());
  protected readonly teamEntries = computed(() => {
    const team = this.pickTeamId();
    return team === null ? [] : teamCalendar(this.allGames(), team).months.flatMap(month => month.games);
  });
  protected readonly allGames = computed(() => currentValue(this.seasonGames) ?? []);
  protected readonly dayGames = computed(() => gamesOn(this.allGames(), this.pickDay()));
  /** Mois affiché par l'agenda : celui du match choisi, puis celui où l'on navigue. */
  protected readonly month = linkedSignal(() => monthOf(this.day()));
  /** Saisie libre d'une affiche (match hypothétique) : repliée par défaut, ouverte si l'URL décrit un match sans identifiant. */
  protected readonly manual = linkedSignal(() => !!this.home() && !this.game());

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

  /** Choisir une équipe propose son prochain match, prédit aussitôt. */
  onPickTeam(value: string): void {
    this.pickTeamId.set(idParam(value) ?? null);
    const entry = defaultGame(this.teamEntries());
    if (entry) {
      this.choose(entry.game);
    } else {
      this.prediction.set(null);
    }
  }

  /** Un match du calendrier remplit le formulaire (équipes, jour, type) et lance la prédiction. */
  choose(game: GameResponse): void {
    if (!game.homeTeam?.id || !game.awayTeam?.id || !game.scheduledDate) {
      return;
    }
    this.homeTeamId.set(game.homeTeam.id);
    this.awayTeamId.set(game.awayTeam.id);
    this.day.set(game.scheduledDate.slice(0, 10));
    this.isPlayoff.set(isPlayoffGame(game));
    this.gameId.set(game.id ?? null);
    void this.predict();
  }

  team(id: number | null) {
    return id === null ? undefined : this.catalog.teamsById().get(id);
  }

  onSeason(value: string): void {
    this.seasonId.set(idParam(value) ?? null);
    this.pickTeamId.set(null);
    this.day.set(defaultPredictionDate(this.catalog.season(this.seasonId())));
    this.reset();
  }

  onTeam(side: 'home' | 'away', value: string): void {
    (side === 'home' ? this.homeTeamId : this.awayTeamId).set(idParam(value) ?? null);
    this.reset();
  }

  swap(): void {
    const home = this.homeTeamId();
    this.homeTeamId.set(this.awayTeamId());
    this.awayTeamId.set(home);
    this.reset();
  }

  /** Le formulaire a changé : la prédiction affichée et le match du calendrier ne correspondent plus. */
  reset(): void {
    this.prediction.set(null);
    this.gameId.set(null);
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
      const result = await this.api.invoke(explainGameOutcome, request);
      this.prediction.set({
        homeTeamId: request.homeTeamId, awayTeamId: request.awayTeamId, date: request.date, playoff: request.playoff, result,
        factors: factorRows(result.factors, request.playoff),
        source: explanationSource(result),
      });
      // L'URL reflète la prédiction affichée : elle se partage et survit au rechargement.
      void this.router.navigate([], {
        queryParams: { game: this.gameId(), season: request.seasonId, home: request.homeTeamId, away: request.awayTeamId, date: request.date, playoff: request.playoff || null },
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

  /** Moins de 10 matchs joués par une des équipes : sa forme repose encore surtout sur la saison passée. */
  protected earlySeason(result: ExplainedPrediction): boolean {
    return Math.min(result.homeGamesPlayed ?? 0, result.awayGamesPlayed ?? 0) < 10;
  }
}
