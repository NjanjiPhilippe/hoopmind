import { monthGrid, monthOf, shiftMonth } from './month-grid';

describe('month-grid', () => {
  it('lays a month out in weeks starting on Monday', () => {
    // Le 1er novembre 2023 est un mercredi ; le 30 est un jeudi.
    const weeks = monthGrid('2023-11');

    expect(weeks).toHaveLength(5);
    expect(weeks[0].map(d => d.date)).toEqual(['2023-10-30', '2023-10-31', '2023-11-01', '2023-11-02', '2023-11-03', '2023-11-04', '2023-11-05']);
    expect(weeks[0][1].inMonth).toBe(false);
    expect(weeks[0][2].inMonth).toBe(true);
    expect(weeks.at(-1)!.at(-1)!.date).toBe('2023-12-03');
  });

  it('moves between months across years', () => {
    expect(shiftMonth('2023-12', 1)).toBe('2024-01');
    expect(shiftMonth('2024-01', -1)).toBe('2023-12');
    expect(monthOf('2024-04-16')).toBe('2024-04');
  });
});
