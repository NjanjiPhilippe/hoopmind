import { DatePipe, DecimalPipe, PercentPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, linkedSignal, resource, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

import { Api } from '../../../core/api/generated/api';
import { getNbaDatabaseStatus } from '../../../core/api/generated/fn/import-des-donnees/get-nba-database-status';
import { importReferenceCsv } from '../../../core/api/generated/fn/import-des-donnees/import-reference-csv';
import { listReferenceDatasets } from '../../../core/api/generated/fn/import-des-donnees/list-reference-datasets';
import { startNbaDatabaseImport } from '../../../core/api/generated/fn/import-des-donnees/start-nba-database-import';
import { getModelMetrics } from '../../../core/api/generated/fn/spark-analytics/get-model-metrics';
import { trainGameOutcomeModel } from '../../../core/api/generated/fn/spark-analytics/train-game-outcome-model';
import { FileReport } from '../../../core/api/generated/models/file-report';
import { TrainingReport } from '../../../core/api/generated/models/training-report';
import { toProblem } from '../../../core/http/problem';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { downloadPercent, featureLabel, formatBytes, isRunning, phaseLabel } from './data-labels';

/** Fréquence de suivi d'un import en arrière-plan. */
const POLL_MS = 2000;
const DEFAULT_TRAINING_SEASONS = 5;

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
  private readonly api = inject(Api);

  protected readonly featureLabel = featureLabel;
  protected readonly formatBytes = formatBytes;
  protected readonly phaseLabel = phaseLabel;

  // Base de Wyatt Walsh
  protected readonly nbaStatus = resource({ loader: () => this.api.invoke(getNbaDatabaseStatus) });
  protected readonly job = computed(() => this.nbaStatus.value()?.job ?? null);
  protected readonly jobRunning = computed(() => isRunning(this.job()));
  protected readonly downloadPercent = computed(() => downloadPercent(this.job()));
  protected readonly importSeasons = linkedSignal(() => this.nbaStatus.value()?.defaultSeasons ?? 10);
  protected readonly nbaError = signal<string | null>(null);
  protected readonly starting = signal(false);

  // Archive CSV de Sumitro Datta
  protected readonly datasets = resource({ loader: () => this.api.invoke(listReferenceDatasets) });
  protected readonly importedDatasets = computed(() => (this.datasets.value() ?? []).filter(d => (d.rows ?? 0) > 0).length);
  protected readonly selectedFiles = signal<File[]>([]);
  protected readonly dragging = signal(false);
  protected readonly uploading = signal(false);
  protected readonly fileReports = signal<FileReport[] | null>(null);
  protected readonly csvError = signal<string | null>(null);

  // Modèle de prédiction
  protected readonly model = resource({ loader: () => this.latestModel() });
  protected readonly trainingSeasons = signal(DEFAULT_TRAINING_SEASONS);
  protected readonly training = signal(false);
  protected readonly modelError = signal<string | null>(null);
  protected readonly weights = computed(() => {
    const weights = Object.entries(this.model.value()?.featureWeights ?? {});
    const max = Math.max(...weights.map(([, w]) => Math.abs(w)), 1e-9);
    return weights.map(([feature, weight]) => ({ feature, weight, width: (Math.abs(weight) / max) * 100 }));
  });

  constructor() {
    // Tant qu'un import tourne, son état est relu régulièrement.
    let timer: ReturnType<typeof setTimeout> | undefined;
    effect(() => {
      clearTimeout(timer);
      // Chaque relecture produit un nouvel état : l'effet se relance tant que l'import n'est pas fini.
      if (isRunning(this.nbaStatus.value()?.job)) {
        timer = setTimeout(() => this.nbaStatus.reload(), POLL_MS);
      }
    });
    inject(DestroyRef).onDestroy(() => clearTimeout(timer));
  }

  async startImport(download: boolean): Promise<void> {
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

  onSeasons(value: string, target: 'import' | 'training'): void {
    const seasons = Number.parseInt(value, 10);
    if (Number.isFinite(seasons) && seasons > 0) {
      (target === 'import' ? this.importSeasons : this.trainingSeasons).set(seasons);
    }
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

  async train(): Promise<void> {
    this.training.set(true);
    this.modelError.set(null);
    try {
      const report = await this.api.invoke(trainGameOutcomeModel, { seasons: this.trainingSeasons() });
      this.model.set(report);
    } catch (e) {
      this.modelError.set(toProblem(e).detail ?? 'Entraînement impossible');
    } finally {
      this.training.set(false);
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
