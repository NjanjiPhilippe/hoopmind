import { PredictionHistoryEntry } from '../../core/api/generated/models/prediction-history-entry';
import { predictParams, summarize } from './prediction-summary';

const entry: PredictionHistoryEntry = {
  id: 1, seasonId: 4, homeTeamId: 10, homeTeam: 'Boston Celtics', awayTeamId: 20, awayTeam: 'New York Knicks',
  gameDate: '2025-01-10', playoff: false, homeWinProbability: 0.3, model: 'game-outcome-logistic-regression',
  predictedAt: '2026-10-26T06:00:00Z',
};

describe('summarize', () => {
  it('names the favourite with its own probability and waits for the game', () => {
    const summary = summarize(entry);
    expect(summary.winner).toBe('New York Knicks');
    expect(summary.probability).toBeCloseTo(0.7);
    expect(summary.predictedScore).toBeNull();
    expect(summary.verdict).toBe('pending');
  });

  it('shows both scores and the verdict once the game is played', () => {
    const summary = summarize({ ...entry, predictedHomePoints: 101, predictedAwayPoints: 108, actualHomePoints: 110,
      actualAwayPoints: 104, correct: false });
    expect(summary.predictedScore).toBe('101-108');
    expect(summary.actualScore).toBe('110-104');
    expect(summary.verdict).toBe('wrong');
  });
});

describe('predictParams', () => {
  it('reopens the same match on the predict page', () => {
    expect(predictParams(entry)).toEqual({ season: 4, home: 10, away: 20, date: '2025-01-10', playoff: false });
  });
});
