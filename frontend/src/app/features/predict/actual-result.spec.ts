import { actualResult } from './actual-result';

describe('actualResult', () => {
  const game = { id: 9, homeTeam: { id: 1 }, awayTeam: { id: 2 }, result: { homeTeamScore: 104, awayTeamScore: 110, overtime: true } };

  it('compares the favourite with the real winner', () => {
    expect(actualResult(game, 1, 2, 0.62)).toEqual({ homeScore: 104, awayScore: 110, homeWon: false, overtime: true, correct: false });
    expect(actualResult(game, 1, 2, 0.4)?.correct).toBe(true);
  });

  it('says nothing for another matchup or a game not played yet', () => {
    expect(actualResult(game, 2, 1, 0.5)).toBeNull();
    expect(actualResult({ ...game, result: undefined }, 1, 2, 0.5)).toBeNull();
    expect(actualResult(null, 1, 2, 0.5)).toBeNull();
  });
});
