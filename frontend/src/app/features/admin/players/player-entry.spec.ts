import { parsePlayer, parseStats, positionLabel, statValue } from './player-entry';

describe('parsePlayer', () => {
  it('reads a player and leaves empty fields out', () => {
    expect(parsePlayer({ firstName: ' Jayson ', lastName: 'Tatum', position: 'SMALL_FORWARD', jerseyNumber: '0', height: '203', weight: '' }))
      .toEqual({ firstName: 'Jayson', lastName: 'Tatum', position: 'SMALL_FORWARD', status: 'ACTIVE', jerseyNumber: 0, height: 203, weight: undefined });
  });

  it('refuses a missing name and an impossible number', () => {
    expect(parsePlayer({ firstName: '', lastName: 'Tatum' })).toEqual({ error: 'Le prénom et le nom sont obligatoires.' });
    expect(parsePlayer({ firstName: 'J', lastName: 'T', jerseyNumber: '120' })).toEqual({ error: 'Le numéro va de 0 à 99.' });
  });
});

describe('parseStats', () => {
  it('turns percentages into fractions and accepts a decimal comma', () => {
    expect(parseStats({ gamesPlayed: '70', gamesStarted: '68', pointsPerGame: '26,9', fieldGoalPercentage: '47.5' }))
      .toMatchObject({ gamesPlayed: 70, gamesStarted: 68, pointsPerGame: 26.9, fieldGoalPercentage: 0.475 });
  });

  it('needs the games played and keeps starts below them', () => {
    expect(parseStats({ gamesPlayed: '' })).toEqual({ error: 'Indiquez le nombre de matchs joués (0 à 110).' });
    expect(parseStats({ gamesPlayed: '10', gamesStarted: '12' })).toEqual({ error: 'Les titularisations vont de 0 au nombre de matchs joués.' });
    expect('error' in parseStats({ gamesPlayed: '10', threePointPercentage: '140' })).toBe(true);
  });
});

describe('labels and values', () => {
  it('names positions in French and shows saved percentages out of 100', () => {
    expect(positionLabel('CENTER')).toBe('Pivot');
    expect(statValue({ playerId: 1, fieldGoalPercentage: 0.475 } as never, 'fieldGoalPercentage', true)).toBe('47.5');
    expect(statValue(null, 'pointsPerGame', false)).toBe('');
  });
});
