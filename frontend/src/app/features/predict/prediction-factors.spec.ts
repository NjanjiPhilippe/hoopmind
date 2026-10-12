import { explanationSource, factorRows, factorValue } from './prediction-factors';

describe('prediction-factors', () => {
  const factors = [
    { feature: 'net_rating_diff', homeValue: 9.44, awayValue: 1.2, contribution: 0.48 },
    { feature: 'rest_days_diff', homeValue: 1, awayValue: 3, contribution: -0.24 },
    { feature: 'is_playoff', homeValue: 0, awayValue: 0, contribution: -0.01 },
  ];

  it('formats each value like a basketball fan reads it', () => {
    expect(factorValue('win_pct_diff', 0.684)).toBe('68\u00a0%');
    expect(factorValue('net_rating_diff', 9.44)).toBe('+9,4');
    expect(factorValue('rest_days_diff', 1)).toBe('1 jour');
    expect(factorValue('rest_days_diff', 3)).toBe('3 jours');
    expect(factorValue('is_playoff', 1)).toBe('oui');
  });

  it('weighs factors against the strongest one and hides the playoff flag in regular season', () => {
    const rows = factorRows(factors, false);

    expect(rows.map(row => row.feature)).toEqual(['net_rating_diff', 'rest_days_diff']);
    expect(rows[0]).toMatchObject({ label: 'Net rating (pour 100 possessions)', home: '+9,4', away: '+1,2', weight: 1, favorsHome: true });
    expect(rows[1]).toMatchObject({ weight: 0.5, favorsHome: false, correction: false });
    expect(rows[0].correction).toBe(false);
    expect(factorRows(factors, true).map(row => row.feature)).toContain('is_playoff');
    expect(factorRows(undefined, false)).toEqual([]);
  });

  it('flags a factor that favors the team with the worse values as a model correction', () => {
    const [row] = factorRows([{ feature: 'win_pct_diff', homeValue: 0.74, awayValue: 0.07, contribution: -0.38 }], false);

    expect(row).toMatchObject({ favorsHome: false, correction: true });
  });

  it('says who wrote the explanation', () => {
    expect(explanationSource({ explanation: { text: '…', provider: 'openai-compatible', model: 'gemini-2.5-flash' } }))
      .toBe('Rédigé par gemini-2.5-flash à partir des seuls chiffres du modèle.');
    expect(explanationSource({ explanation: { text: '…', provider: 'template' } })).toContain('automatiquement');
    expect(explanationSource({ explanation: { text: '…', provider: 'template' }, fallback: true })).toContain('n\'a pas répondu');
  });
});
