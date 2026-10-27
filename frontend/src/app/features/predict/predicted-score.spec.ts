import { marginMiss, points, predictedScore } from './predicted-score';

describe('predictedScore', () => {
  const score = {
    homePoints: 114, awayPoints: 107, margin: 7.2, total: 221.6, marginError: 10.4,
    homePointsPerGame: 116.1, homePointsAllowedPerGame: 108.5, awayPointsPerGame: 111, awayPointsAllowedPerGame: 112.4,
  };

  it('rounds the margin, the total and the model error', () => {
    expect(predictedScore({ homeWinProbability: 0.7, score })).toEqual(expect.objectContaining({
      homePoints: 114, awayPoints: 107, homeLeads: true, margin: 7, total: 222, marginError: 10, agreesWithFavorite: true,
    }));
  });

  it('flags a score that disagrees with the favourite', () => {
    const close = { ...score, homePoints: 110, awayPoints: 111, margin: -0.4 };
    expect(predictedScore({ homeWinProbability: 0.53, score: close })).toEqual(expect.objectContaining({
      homeLeads: false, margin: 1, agreesWithFavorite: false,
    }));
  });

  it('is null before the score model is trained', () => {
    expect(predictedScore({ homeWinProbability: 0.6 })).toBeNull();
    expect(predictedScore(undefined)).toBeNull();
  });

  it('measures how far the margin was from the real one', () => {
    const predicted = predictedScore({ homeWinProbability: 0.7, score })!;
    expect(marginMiss(predicted, 104, 110)).toBe(13);
    expect(marginMiss(predicted, 120, 113)).toBe(0);
  });

  it('writes points in French', () => {
    expect(points(1)).toBe('1 point');
    expect(points(12)).toBe('12 points');
  });
});
