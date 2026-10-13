import { teamCalendar } from './team-calendar';

describe('teamCalendar', () => {
  const GSW = 1;
  const games = [
    { id: 3, scheduledDate: '2023-11-02T00:00:00', homeTeam: { id: 2 }, awayTeam: { id: GSW } },
    { id: 1, scheduledDate: '2023-10-24T00:00:00', homeTeam: { id: GSW }, awayTeam: { id: 2 },
      result: { homeTeamScore: 104, awayTeamScore: 108 } },
    { id: 2, scheduledDate: '2023-10-27T00:00:00', homeTeam: { id: 3 }, awayTeam: { id: GSW },
      result: { homeTeamScore: 120, awayTeamScore: 130 } },
    { id: 4, scheduledDate: '2023-11-05T00:00:00', homeTeam: { id: GSW }, awayTeam: { id: 3 } },
    { id: 5, scheduledDate: '2023-11-06T00:00:00', homeTeam: { id: 2 }, awayTeam: { id: 3 } },
  ];

  it('lists the team games month by month, from the team side', () => {
    const calendar = teamCalendar(games, GSW);

    expect(calendar.months.map(m => m.month)).toEqual(['2023-10', '2023-11']);
    expect(calendar.months[0].games.map(g => g.game.id)).toEqual([1, 2]);
    expect(calendar.months[0].games[0]).toMatchObject({ home: true, opponent: { id: 2 }, teamScore: 104, opponentScore: 108, outcome: 'D' });
    expect(calendar.months[0].games[1]).toMatchObject({ home: false, opponent: { id: 3 }, teamScore: 130, outcome: 'V' });
    expect(calendar.wins).toBe(1);
    expect(calendar.losses).toBe(1);
  });

  it('flags the first game still to play', () => {
    const november = teamCalendar(games, GSW).months[1].games;

    expect(november.map(g => [g.game.id, g.outcome, g.next])).toEqual([[3, null, true], [4, null, false]]);
  });
});
