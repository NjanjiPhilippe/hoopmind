import { JobStatus } from '../../../core/api/generated/models/job-status';
import { TrainingJobStatus } from '../../../core/api/generated/models/training-job-status';
import { TrainingReport } from '../../../core/api/generated/models/training-report';

/** Variables du modèle de prédiction des matchs, en clair. */
const FEATURE_LABELS: Record<string, string> = {
  win_pct: '% de victoires',
  last10_win_pct: '% de victoires sur les 10 derniers matchs',
  net_rating: 'net rating',
  offensive_rating: 'rating offensif',
  defensive_rating: 'rating défensif',
  rest_days: 'jours de repos',
  point_diff: 'écart de points moyen',
  is_playoff: 'Match de playoffs',
};

/**
 * Libellé d'une variable. Le modèle actuel n'utilise que des écarts « domicile moins extérieur » ; les variables
 * par équipe ({@code home_…}, {@code away_…}) restent lisibles pour les modèles entraînés avant ce changement.
 */
export function featureLabel(feature: string): string {
  const side = feature.startsWith('home_') ? 'Domicile' : feature.startsWith('away_') ? 'Extérieur' : null;
  const base = feature.replace(/^(home|away)_/, '').replace(/_diff$/, '');
  const label = FEATURE_LABELS[base] ?? base;
  if (side) {
    return `${side} : ${capitalize(label)}`;
  }
  return feature.endsWith('_diff') ? `Écart de ${label}` : capitalize(label);
}

function capitalize(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1);
}

const PHASE_LABELS: Record<NonNullable<JobStatus['phase']>, string> = {
  DOWNLOADING: 'Téléchargement depuis Kaggle',
  IMPORTING: 'Import en base',
  SUCCEEDED: 'Import terminé',
  FAILED: 'Échec',
  INTERRUPTED: 'Interrompu (serveur redémarré)',
};

export function phaseLabel(phase: JobStatus['phase']): string {
  return phase ? PHASE_LABELS[phase] : '';
}

const TRAINING_PHASE_LABELS: Record<NonNullable<TrainingJobStatus['phase']>, string> = {
  TRAINING: 'Entraînement en cours',
  SUCCEEDED: 'Entraînement terminé',
  FAILED: 'Échec de l\'entraînement',
  INTERRUPTED: 'Entraînement interrompu (serveur redémarré)',
};

export function trainingPhaseLabel(phase: TrainingJobStatus['phase']): string {
  return phase ? TRAINING_PHASE_LABELS[phase] : '';
}

export function isRunning(job: JobStatus | null | undefined): boolean {
  return job?.phase === 'DOWNLOADING' || job?.phase === 'IMPORTING';
}

export function isTraining(job: TrainingJobStatus | null | undefined): boolean {
  return job?.phase === 'TRAINING';
}

/** Un nombre de saisons saisi est valide s'il est entier et compris entre 1 et {@code max}. */
export function validSeasons(value: number, max: number): boolean {
  return Number.isInteger(value) && value >= 1 && value <= max;
}

/**
 * Écart entre le modèle et la meilleure des deux règles de référence, en points de précision. Négatif quand le modèle
 * fait moins bien qu'une règle simple : l'interface le signale au lieu de le laisser passer.
 */
export function gainOverBaseline(report: TrainingReport | null | undefined): number | null {
  if (report?.accuracy == null) {
    return null;
  }
  const baselines = [report.homeTeamBaseline, report.betterRecordBaseline].filter((b): b is number => b != null);
  if (baselines.length === 0) {
    return null;
  }
  return report.accuracy - Math.max(...baselines);
}

export function formatDuration(ms: number | null | undefined): string {
  if (ms == null) {
    return '';
  }
  if (ms < 1000) {
    return 'moins d\'une seconde';
  }
  const seconds = Math.round(ms / 1000);
  return seconds < 60 ? `${seconds} s` : `${Math.floor(seconds / 60)} min ${String(seconds % 60).padStart(2, '0')} s`;
}

export function formatBytes(bytes: number | null | undefined): string {
  if (bytes == null) {
    return '';
  }
  const units = ['o', 'Ko', 'Mo', 'Go'];
  let value = bytes;
  let unit = 0;
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024;
    unit++;
  }
  return `${unit === 0 ? value : value.toFixed(1).replace('.', ',')} ${units[unit]}`;
}

/** Part du téléchargement effectuée (0 à 100), ou null si la taille totale est inconnue. */
export function downloadPercent(job: JobStatus | null | undefined): number | null {
  if (!job?.totalBytes || job.downloadedBytes == null) {
    return null;
  }
  return Math.min(100, Math.round((job.downloadedBytes / job.totalBytes) * 100));
}
