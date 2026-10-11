import { filterGames } from './games.component';

describe('filterGames', () => {
  const games = [
    { id: 1, scheduledDate: '2024-10-22T00:00:00', homeTeam: { id: 1 }, awayTeam: { id: 2 } },
    { id: 2, scheduledDate: '2024-10-25T00:00:00', homeTeam: { id: 3 }, awayTeam: { id: 1 } },
    { id: 3, scheduledDate: '2024-10-24T00:00:00', homeTeam: { id: 2 }, awayTeam: { id: 3 } },
  ];

  it('keeps every game of the season, latest first', () => {
    expect(filterGames(games, null).map(g => g.id)).toEqual([2, 3, 1]);
  });

  it('keeps the home and away games of the chosen team', () => {
    expect(filterGames(games, 1).map(g => g.id)).toEqual([2, 1]);
  });
});
