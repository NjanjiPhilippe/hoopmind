import { calendarLabel, defaultGame, gamesOn } from './game-picker';
import { teamCalendar } from '../games/team-calendar';

describe('game-picker', () => {
  const GSW = { id: 1, city: 'Golden State', name: 'Warriors' };
  const LAC = { id: 2, city: 'Los Angeles', name: 'Clippers' };
  const games = [
    { id: 1, scheduledDate: '2023-11-02T00:00:00', homeTeam: LAC, awayTeam: GSW, result: { homeTeamScore: 112, awayTeamScore: 115 } },
    { id: 2, scheduledDate: '2023-11-02T00:00:00', homeTeam: { id: 3 }, awayTeam: { id: 4 } },
    { id: 3, scheduledDate: '2023-11-05T00:00:00', homeTeam: GSW, awayTeam: LAC },
  ];

  it('lists the games of a day', () => {
    expect(gamesOn(games, '2023-11-02').map(g => g.id)).toEqual([1, 2]);
    expect(gamesOn(games, '2023-11-03')).toEqual([]);
  });

  it('labels a game from the team side and proposes its next game', () => {
    const entries = teamCalendar(games, 1).months.flatMap(month => month.games);
    const date = (value: string) => value.slice(5, 10);

    expect(entries.map(entry => calendarLabel(entry, date))).toEqual([
      '11-02 · @ Los Angeles Clippers · V 115-112',
      '11-05 · vs Los Angeles Clippers · prochain match',
    ]);
    expect(defaultGame(entries)?.game.id).toBe(3);
    expect(defaultGame(entries.slice(0, 1))?.game.id).toBe(1);
    expect(defaultGame([])).toBeUndefined();
  });
});
