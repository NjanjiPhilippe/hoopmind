import { gamesOn, parseScore, shiftDay } from './results-entry';

describe('gamesOn', () => {
  it('garde les matchs du jour, dans l’ordre de l’heure', () => {
    const games = [
      { id: 3, scheduledDate: '2026-10-24T22:00:00' },
      { id: 1, scheduledDate: '2026-10-23T19:00:00' },
      { id: 2, scheduledDate: '2026-10-24T19:30:00' },
    ];
    expect(gamesOn(games, '2026-10-24').map(game => game.id)).toEqual([2, 3]);
  });
});

describe('parseScore', () => {
  it('accepte deux scores différents et les prolongations', () => {
    expect(parseScore('112', '108', '1')).toEqual({ homeTeamScore: 112, awayTeamScore: 108, overtimePeriods: 1 });
  });

  it('refuse un score manquant, négatif ou une égalité', () => {
    expect(parseScore('', '100', '0')).toEqual({ error: 'Saisissez les deux scores.' });
    expect(parseScore('-2', '100', '0')).toEqual({ error: 'Un score va de 0 à 250 points.' });
    expect(parseScore('100', '100', '0')).toEqual({ error: 'Un match NBA ne se termine pas sur une égalité.' });
  });
});

describe('shiftDay', () => {
  it('passe d’un mois à l’autre', () => {
    expect(shiftDay('2026-10-31', 1)).toBe('2026-11-01');
    expect(shiftDay('2026-11-01', -1)).toBe('2026-10-31');
  });
});
