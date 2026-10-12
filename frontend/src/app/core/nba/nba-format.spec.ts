import { byDateDesc, defaultPredictionDate, gameTypeLabel, streakLabel, today } from './nba-format';

describe('nba-format', () => {
  const season = { startDate: '2024-10-22', endDate: '2025-04-13' };

  it('proposes today during the season, otherwise its closest bound', () => {
    expect(defaultPredictionDate(season, new Date('2025-01-15T12:00:00'))).toBe('2025-01-15');
    expect(defaultPredictionDate(season, new Date('2025-08-01T12:00:00'))).toBe('2025-04-13');
    expect(defaultPredictionDate(season, new Date('2024-09-01T12:00:00'))).toBe('2024-10-22');
    expect(defaultPredictionDate(null, new Date('2025-08-01T12:00:00'))).toBe(today(new Date('2025-08-01T12:00:00')));
  });

  it('labels only the games that are not regular season', () => {
    expect(gameTypeLabel('PLAYOFFS')).toBe('Playoffs');
    expect(gameTypeLabel('FINALS')).toBe('Finales');
    expect(gameTypeLabel('REGULAR_SEASON')).toBe('');
    expect(gameTypeLabel(undefined)).toBe('');
  });

  it('describes streaks and sorts games from the latest', () => {
    expect(streakLabel(3)).toBe('3 V');
    expect(streakLabel(-2)).toBe('2 D');
    expect(streakLabel(0)).toBe('');
    const games = [{ scheduledDate: '2024-10-22T00:00:00' }, { scheduledDate: '2024-11-02T00:00:00' }];
    expect(games.sort(byDateDesc)[0].scheduledDate).toBe('2024-11-02T00:00:00');
  });
});
