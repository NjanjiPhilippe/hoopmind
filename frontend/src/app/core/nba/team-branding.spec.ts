import { matchupColors, teamColor, teamLogoUrl, textOn } from './team-branding';

describe('team-branding', () => {
  it('gives each franchise its official colour and NBA logo', () => {
    expect(teamColor({ abbreviation: 'GSW' })).toBe('#1D428A');
    expect(teamColor({ abbreviation: 'xyz' })).toBe('#6B7280');
    expect(teamLogoUrl({ abbreviation: 'BOS' })).toBe('https://cdn.nba.com/logos/nba/1610612738/global/L/logo.svg');
    expect(teamLogoUrl({ abbreviation: 'BOS', logoUrl: '/logos/bos.svg' })).toBe('/logos/bos.svg');
    expect(teamLogoUrl({ abbreviation: 'SEA' })).toBeNull();
    expect(teamLogoUrl(null)).toBeNull();
  });

  it('keeps the two teams of a game apart', () => {
    expect(matchupColors({ abbreviation: 'LAC' }, { abbreviation: 'GSW' })).toEqual({ home: '#C8102E', away: '#1D428A' });
    // Bulls et Raptors ont le même rouge : l'équipe à l'extérieur prend sa couleur secondaire.
    expect(matchupColors({ abbreviation: 'CHI' }, { abbreviation: 'TOR' })).toEqual({ home: '#CE1141', away: '#000000' });
  });

  it('writes in white on dark colours and in black on light ones', () => {
    expect(textOn('#1D428A')).toBe('#FFFFFF');
    expect(textOn('#FFC72C')).toBe('#111827');
  });
});
