import { splitGames } from './dashboard.component';

describe('splitGames', () => {
  it('returns the latest results and the next scheduled games', () => {
    const { results, upcoming } = splitGames([
      { id: 1, scheduledDate: '2024-10-22T00:00:00', status: 'COMPLETED', result: { homeTeamScore: 100 } },
      { id: 2, scheduledDate: '2024-10-24T00:00:00', status: 'COMPLETED', result: { homeTeamScore: 90 } },
      { id: 3, scheduledDate: '2024-10-30T00:00:00', status: 'SCHEDULED' },
      { id: 4, scheduledDate: '2024-10-27T00:00:00', status: 'SCHEDULED' },
    ], 5);

    expect(results.map(g => g.id)).toEqual([2, 1]);
    expect(upcoming.map(g => g.id)).toEqual([4, 3]);
  });
});
