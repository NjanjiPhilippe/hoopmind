import { downloadPercent, featureLabel, formatBytes, isRunning, phaseLabel } from './data-labels';

describe('data-labels', () => {
  it('names the model features in French', () => {
    expect(featureLabel('home_win_pct')).toBe('Domicile : % de victoires');
    expect(featureLabel('away_rest_days')).toBe('Extérieur : Jours de repos');
    expect(featureLabel('net_rating_diff')).toBe('Écart : Net rating');
    expect(featureLabel('is_playoff')).toBe('Match de playoffs');
    expect(featureLabel('unknown_feature')).toBe('unknown_feature');
  });

  it('formats sizes and download progress', () => {
    expect(formatBytes(512)).toBe('512 o');
    expect(formatBytes(1536)).toBe('1,5 Ko');
    expect(formatBytes(2.5 * 1024 ** 3)).toBe('2,5 Go');
    expect(formatBytes(undefined)).toBe('');
    expect(downloadPercent({ downloadedBytes: 50, totalBytes: 200 })).toBe(25);
    expect(downloadPercent({ downloadedBytes: 50 })).toBeNull();
  });

  it('knows when an import is still running', () => {
    expect(isRunning({ phase: 'DOWNLOADING' })).toBe(true);
    expect(isRunning({ phase: 'IMPORTING' })).toBe(true);
    expect(isRunning({ phase: 'FAILED' })).toBe(false);
    expect(isRunning(null)).toBe(false);
    expect(phaseLabel('SUCCEEDED')).toBe('Import terminé');
  });
});
