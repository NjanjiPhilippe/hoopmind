import { byConference } from './standings.component';

describe('byConference', () => {
  const teams = new Map([
    [1, { id: 1, conference: 'EASTERN' as const }],
    [2, { id: 2, conference: 'EASTERN' as const }],
    [3, { id: 3, conference: 'WESTERN' as const }],
  ]);

  it('splits the standings by conference and ranks them by win percentage', () => {
    const groups = byConference([
      { team: { id: 1 }, winPercentage: 0.4, wins: 4 },
      { team: { id: 3 }, winPercentage: 0.7, wins: 7 },
      { team: { id: 2 }, winPercentage: 0.6, wins: 6 },
    ], teams);

    expect(groups.map(g => g.label)).toEqual(['Conférence Est', 'Conférence Ouest']);
    expect(groups[0].rows.map(r => r.team?.id)).toEqual([2, 1]);
    expect(groups[1].rows.map(r => r.team?.id)).toEqual([3]);
  });

  it('leaves out a conference without any team record', () => {
    expect(byConference([{ team: { id: 3 }, winPercentage: 0.5 }], teams).map(g => g.conference)).toEqual(['WESTERN']);
  });
});
