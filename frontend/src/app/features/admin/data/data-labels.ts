import { JobStatus } from '../../../core/api/generated/models/job-status';

/** Variables du modèle de prédiction des matchs, en clair. */
const FEATURE_LABELS: Record<string, string> = {
  win_pct: '% de victoires',
  last10_win_pct: '% de victoires (10 derniers matchs)',
  net_rating: 'Net rating',
  offensive_rating: 'Rating offensif',
  defensive_rating: 'Rating défensif',
  rest_days: 'Jours de repos',
  point_diff: 'Écart de points moyen',
  is_playoff: 'Match de playoffs',
};

export function featureLabel(feature: string): string {
  const side = feature.startsWith('home_') ? 'Domicile' : feature.startsWith('away_') ? 'Extérieur' : null;
  const base = feature.replace(/^(home|away)_/, '').replace(/_diff$/, '');
  const label = FEATURE_LABELS[base] ?? base;
  if (side) {
    return `${side} : ${label}`;
  }
  return feature.endsWith('_diff') ? `Écart : ${label}` : label;
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

export function isRunning(job: JobStatus | null | undefined): boolean {
  return job?.phase === 'DOWNLOADING' || job?.phase === 'IMPORTING';
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
