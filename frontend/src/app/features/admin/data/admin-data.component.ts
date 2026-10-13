import { DatePipe, DecimalPipe, PercentPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, linkedSignal, resource, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

import { Api } from '../../../core/api/generated/api';
import { getEoinMooreStatus } from '../../../core/api/generated/fn/import-des-donnees/get-eoin-moore-status';
import { getNbaDatabaseStatus } from '../../../core/api/generated/fn/import-des-donnees/get-nba-database-status';
import { getStatsNbaStatus } from '../../../core/api/generated/fn/import-des-donnees/get-stats-nba-status';
import { importReferenceCsv } from '../../../core/api/generated/fn/import-des-donnees/import-reference-csv';
import { listReferenceDatasets } from '../../../core/api/generated/fn/import-des-donnees/list-reference-datasets';
import { startEoinMooreImport } from '../../../core/api/generated/fn/import-des-donnees/start-eoin-moore-import';
import { startNbaDatabaseImport } from '../../../core/api/generated/fn/import-des-donnees/start-nba-database-import';
import { startStatsNbaImport } from '../../../core/api/generated/fn/import-des-donnees/start-stats-nba-import';
import { getModelMetrics } from '../../../core/api/generated/fn/spark-analytics/get-model-metrics';
import { getGameOutcomeTrainingStatus } from '../../../core/api/generated/fn/spark-analytics/get-game-outcome-training-status';
import { startGameOutcomeTraining } from '../../../core/api/generated/fn/spark-analytics/start-game-outcome-training';
import { FileReport } from '../../../core/api/generated/models/file-report';
import { TrainingReport } from '../../../core/api/generated/models/training-report';
import { toProblem } from '../../../core/http/problem';
import { pollWhile } from '../../../core/util/poll';
import { currentValue } from '../../../core/util/resource';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import {
  downloadPercent,
  eoinMoorePhaseLabel,
  featureLabel,
  fetchPercent,
  formatBytes,
  formatDuration,
  gainOverBaseline,
  isEoinMooreRunning,
  isRunning,
  isStatsNbaRunning,
  isTraining,
  phaseLabel,
  seasonName,
  statsNbaPhaseLabel,
  trainingPhaseLabel,
  validSeasonRange,
  validSeasons,
} from './data-labels';

/** Fréquence de suivi d'un import ou d'un entraînement en arrière-plan. */
const POLL_MS = 2000;
const DEFAULT_TRAINING_SEASONS = 5;
/** Limites du serveur pour l'entraînement (GameOutcomeModelService). */
const MAX_TRAINING_SEASONS = 30;
/** Limite du serveur pour l'import, tant que l'état de la base n'est pas chargé. */
const MAX_IMPORT_SEASONS = 80;

/**
 * Page « Données » de l'administration : import de la base de Wyatt Walsh (téléchargée par le serveur depuis Kaggle),
 * dépôt des CSV de Sumitro Datta, et entraînement du modèle de prédiction des matchs.
 */
@Component({
  selector: 'app-admin-data',
  imports: [DatePipe, DecimalPipe, PercentPipe, NavbarComponent],
  templateUrl: './admin-data.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminDataComponent {
  protected readonly currentValue = currentValue;
  private readonly api = inject(Api);

  protected readonly featureLabel = featureLabel;
  protected readonly formatBytes = formatBytes;
  protected readonly phaseLabel = phaseLabel;
  protected readonly statsNbaPhaseLabel = statsNbaPhaseLabel;
  protected readonly eoinMoorePhaseLabel = eoinMoorePhaseLabel;
  protected readonly seasonName = seasonName;
  protected readonly trainingPhaseLabel = trainingPhaseLabel;
  protected readonly formatDuration = formatDuration;
  protected readonly maxTrainingSeasons = MAX_TRAINING_SEASONS;

  // Base de Wyatt Walsh
  protected readonly nbaStatus = resource({ loader: () => this.api.invoke(getNbaDatabaseStatus) });
  protected readonly job = computed(() => currentValue(this.nbaStatus)?.job ?? null);
  protected readonly jobRunning = computed(() => isRunning(this.job()));
  protected readonly downloadPercent = computed(() => downloadPercent(this.job()));
  protected readonly importSeasons = linkedSignal(() => currentValue(this.nbaStatus)?.defaultSeasons ?? 10);
  protected readonly maxImportSeasons = computed(() => currentValue(this.nbaStatus)?.maxSeasons ?? MAX_IMPORT_SEASONS);
  protected readonly importSeasonsValid = computed(() => validSeasons(this.importSeasons(), this.maxImportSeasons()));
  protected readonly nbaError = signal<string | null>(null);
  protected readonly starting = signal(false);

  // Saisons récentes lues dans les fichiers d'Eoin Moore (Kaggle), après la fin de la base Walsh
  protected readonly eoinMoore = resource({ loader: () => this.api.invoke(getEoinMooreStatus) });
  protected readonly eoinJob = computed(() => currentValue(this.eoinMoore)?.job ?? null);
  protected readonly eoinRunning = computed(() => isEoinMooreRunning(this.eoinJob()));
  protected readonly eoinFrom = linkedSignal(() => currentValue(this.eoinMoore)?.defaultFromYear ?? 2023);
  protected readonly eoinTo = linkedSignal(() => this.eoinFrom());
  protected readonly eoinRangeValid = computed(() =>
    validSeasonRange(this.eoinFrom(), this.eoinTo(), currentValue(this.eoinMoore)?.maxSeasons ?? 10));
  protected readonly eoinError = signal<string | null>(null);
  protected readonly startingEoin = signal(false);

  // Saisons récentes lues sur stats.nba.com, après la fin de la base Walsh
  protected readonly statsNba = resource({ loader: () => this.api.invoke(getStatsNbaStatus) });
  protected readonly statsJob = computed(() => currentValue(this.statsNba)?.job ?? null);
  protected readonly statsRunning = computed(() => isStatsNbaRunning(this.statsJob()));
  protected readonly fetchPercent = computed(() => fetchPercent(this.statsJob()));
  protected readonly statsError = signal<string | null>(null);
  protected readonly startingStats = signal(false);

  // Archive CSV de Sumitro Datta
  protected readonly datasets = resource({ loader: () => this.api.invoke(listReferenceDatasets) });
  protected readonly importedDatasets = computed(() => (currentValue(this.datasets) ?? []).filter(d => (d.rows ?? 0) > 0).length);
  protected readonly selectedFiles = signal<File[]>([]);
  protected readonly dragging = signal(false);
  protected readonly uploading = signal(false);
  protected readonly fileReports = signal<FileReport[] | null>(null);
  protected readonly csvError = signal<string | null>(null);

  // Modèle de prédiction : dernier modèle enregistré et dernier entraînement lancé en arrière-plan
  protected readonly model = resource({ loader: () => this.latestModel() });
  protected readonly trainingJob = resource({ loader: () => this.api.invoke(getGameOutcomeTrainingStatus) });
  protected readonly training = computed(() => isTraining(currentValue(this.trainingJob)));
  protected readonly trainingSeasons = signal(DEFAULT_TRAINING_SEASONS);
  protected readonly trainingSeasonsValid = computed(() => validSeasons(this.trainingSeasons(), MAX_TRAINING_SEASONS));
  protected readonly startingTraining = signal(false);
  protected readonly modelError = signal<string | null>(null);
  /** Le rapport du job qui vient de finir, sinon le dernier modèle enregistré. */
  protected readonly report = computed(() => {
    const job = currentValue(this.trainingJob);
    return job?.phase === 'SUCCEEDED' && job.report ? job.report : currentValue(this.model);
  });
  protected readonly gain = computed(() => gainOverBaseline(this.report()));

  /** Le serveur n'exécute qu'une opération sur les données à la fois : import et entraînement s'excluent. */
  protected readonly dataBusy = computed(() => this.jobRunning() || this.statsRunning() || this.eoinRunning() || this.training());

  protected readonly weights = computed(() => {
    const weights = Object.entries(this.report()?.featureWeights ?? {});
    const max = Math.max(...weights.map(([, w]) => Math.abs(w)), 1e-9);
    // Du poids le plus fort au plus faible : la variable qui pèse le plus se lit en premier.
    return weights
      .map(([feature, weight]) => ({ feature, weight, width: (Math.abs(weight) / max) * 100 }))
      .sort((a, b) => Math.abs(b.weight) - Math.abs(a.weight));
  });

  constructor() {
    // Tant qu'un import ou un entraînement tourne, son état est relu régulièrement.
    pollWhile(() => isRunning(currentValue(this.nbaStatus)?.job), () => this.nbaStatus.reload(), POLL_MS);
    pollWhile(() => isEoinMooreRunning(currentValue(this.eoinMoore)?.job), () => this.eoinMoore.reload(), POLL_MS);
    pollWhile(() => isStatsNbaRunning(currentValue(this.statsNba)?.job), () => this.statsNba.reload(), POLL_MS);
    pollWhile(() => isTraining(currentValue(this.trainingJob)), () => this.trainingJob.reload(), POLL_MS);
  }

  async startImport(download: boolean): Promise<void> {
    if (!this.importSeasonsValid()) {
      return;
    }
    this.starting.set(true);
    this.nbaError.set(null);
    try {
      await this.api.invoke(startNbaDatabaseImport, { seasons: this.importSeasons(), download });
      this.nbaStatus.reload();
    } catch (e) {
      this.nbaError.set(toProblem(e).detail ?? 'Import impossible');
    } finally {
      this.starting.set(false);
    }
  }

  /** Importe les saisons choisies depuis Games.csv et TeamStatistics.csv, déposés dans le dossier du serveur. */
  async startEoinMoore(): Promise<void> {
    if (!this.eoinRangeValid()) {
      return;
    }
    this.startingEoin.set(true);
    this.eoinError.set(null);
    try {
      await this.api.invoke(startEoinMooreImport, { fromYear: this.eoinFrom(), toYear: this.eoinTo() });
      this.eoinMoore.reload();
    } catch (e) {
      this.eoinError.set(toProblem(e).detail ?? 'Import impossible');
    } finally {
      this.startingEoin.set(false);
    }
  }

  /** La saisie est gardée telle quelle (même invalide) pour que le message d'erreur reste sous le champ. */
  onEoinYear(value: string, target: 'from' | 'to'): void {
    (target === 'from' ? this.eoinFrom : this.eoinTo).set(value === '' ? Number.NaN : Number(value));
  }

  /** Lit sur stats.nba.com les saisons qui manquent, de la dernière saison en base à la saison en cours. */
  async startStatsNba(): Promise<void> {
    this.startingStats.set(true);
    this.statsError.set(null);
    try {
      await this.api.invoke(startStatsNbaImport, {});
      this.statsNba.reload();
    } catch (e) {
      this.statsError.set(toProblem(e).detail ?? 'Import impossible');
    } finally {
      this.startingStats.set(false);
    }
  }

  /** La saisie est gardée telle quelle (même invalide) pour que le message d'erreur reste sous le champ. */
  onSeasons(value: string, target: 'import' | 'training'): void {
    (target === 'import' ? this.importSeasons : this.trainingSeasons).set(value === '' ? Number.NaN : Number(value));
  }

  onFilesSelected(input: HTMLInputElement): void {
    this.addFiles(input.files);
    input.value = '';
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(false);
    this.addFiles(event.dataTransfer?.files ?? null);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(true);
  }

  removeFile(file: File): void {
    this.selectedFiles.update(files => files.filter(f => f !== file));
  }

  async uploadFiles(): Promise<void> {
    const files = this.selectedFiles();
    if (files.length === 0) {
      return;
    }
    this.uploading.set(true);
    this.csvError.set(null);
    this.fileReports.set(null);
    try {
      this.fileReports.set(await this.api.invoke(importReferenceCsv, { body: { files } }));
      this.selectedFiles.set([]);
      this.datasets.reload();
    } catch (e) {
      this.csvError.set(toProblem(e).detail ?? 'Import impossible');
    } finally {
      this.uploading.set(false);
    }
  }

  /** Lance l'entraînement en arrière-plan ; la réponse (202) est le job, dont l'état est ensuite relu. */
  async train(): Promise<void> {
    if (!this.trainingSeasonsValid()) {
      return;
    }
    this.startingTraining.set(true);
    this.modelError.set(null);
    try {
      this.trainingJob.set(await this.api.invoke(startGameOutcomeTraining, { seasons: this.trainingSeasons() }));
    } catch (e) {
      this.modelError.set(toProblem(e).detail ?? 'Entraînement impossible');
    } finally {
      this.startingTraining.set(false);
    }
  }

  private addFiles(list: FileList | null): void {
    const accepted = Array.from(list ?? []).filter(f => /\.(csv|zip)$/i.test(f.name));
    const known = new Set(this.selectedFiles().map(f => f.name + f.size));
    this.selectedFiles.update(files => [...files, ...accepted.filter(f => !known.has(f.name + f.size))]);
  }

  /** Dernier modèle entraîné ; aucun (404) tant qu'il n'a jamais été entraîné, ou si Spark est désactivé. */
  private async latestModel(): Promise<TrainingReport | null> {
    try {
      return await this.api.invoke(getModelMetrics);
    } catch (e) {
      if (e instanceof HttpErrorResponse && e.status === 404) {
        return null;
      }
      throw e;
    }
  }
}
