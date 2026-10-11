import {
  downloadPercent,
  featureLabel,
  formatBytes,
  formatDuration,
  gainOverBaseline,
  isRunning,
  isTraining,
  phaseLabel,
  trainingPhaseLabel,
  validSeasons,
} from './data-labels';

describe('data-labels', () => {
  it('names the model features in French', () => {
    expect(featureLabel('win_pct_diff')).toBe('Écart de % de victoires');
    expect(featureLabel('net_rating_diff')).toBe('Écart de net rating');
    expect(featureLabel('is_playoff')).toBe('Match de playoffs');
    expect(featureLabel('unknown_feature')).toBe('Unknown_feature');
  });

  it('still names the features of models trained before the switch to differences', () => {
    expect(featureLabel('home_win_pct')).toBe('Domicile : % de victoires');
    expect(featureLabel('away_rest_days')).toBe('Extérieur : Jours de repos');
  });

  it('formats sizes, durations and download progress', () => {
    expect(formatBytes(512)).toBe('512 o');
    expect(formatBytes(1536)).toBe('1,5 Ko');
    expect(formatBytes(2.5 * 1024 ** 3)).toBe('2,5 Go');
    expect(formatBytes(undefined)).toBe('');
    expect(formatDuration(300)).toBe('moins d\'une seconde');
    expect(formatDuration(42_400)).toBe('42 s');
    expect(formatDuration(125_000)).toBe('2 min 05 s');
    expect(downloadPercent({ downloadedBytes: 50, totalBytes: 200 })).toBe(25);
    expect(downloadPercent({ downloadedBytes: 50 })).toBeNull();
  });

  it('knows when an import or a training run is still running', () => {
    expect(isRunning({ phase: 'DOWNLOADING' })).toBe(true);
    expect(isRunning({ phase: 'IMPORTING' })).toBe(true);
    expect(isRunning({ phase: 'FAILED' })).toBe(false);
    expect(isRunning(null)).toBe(false);
    expect(phaseLabel('SUCCEEDED')).toBe('Import terminé');
    expect(isTraining({ phase: 'TRAINING' })).toBe(true);
    expect(isTraining({ phase: 'INTERRUPTED' })).toBe(false);
    expect(trainingPhaseLabel('FAILED')).toBe('Échec de l\'entraînement');
  });

  it('checks the number of seasons before calling the server', () => {
    expect(validSeasons(5, 30)).toBe(true);
    expect(validSeasons(50, 30)).toBe(false);
    expect(validSeasons(0, 30)).toBe(false);
    expect(validSeasons(2.5, 30)).toBe(false);
    expect(validSeasons(Number.NaN, 30)).toBe(false);
  });

  it('compares the model with the best simple rule', () => {
    expect(gainOverBaseline({ accuracy: 0.556, homeTeamBaseline: 0.6, betterRecordBaseline: 0.667 })).toBeCloseTo(-0.111);
    expect(gainOverBaseline({ accuracy: 0.752, homeTeamBaseline: 0.55, betterRecordBaseline: 0.7 })).toBeCloseTo(0.052);
    expect(gainOverBaseline({ accuracy: 0.7 })).toBeNull();
    expect(gainOverBaseline(null)).toBeNull();
  });
});
