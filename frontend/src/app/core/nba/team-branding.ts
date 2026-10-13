import { TeamResponse } from '../api/generated/models/team-response';
import { TeamSummaryResponse } from '../api/generated/models/team-summary-response';

type Team = TeamResponse | TeamSummaryResponse | null | undefined;

interface Branding {
  primary: string;
  secondary: string;
}

/**
 * Couleurs officielles des trente franchises (primaire, secondaire), par abréviation. Les secondaires très claires
 * (blanc, argent) sont remplacées par un gris lisible : elles servent de couleur de repli quand deux équipes se
 * ressemblent.
 */
const BRANDING: Record<string, Branding> = {
  ATL: { primary: '#E03A3E', secondary: '#26282A' },
  BOS: { primary: '#007A33', secondary: '#BA9653' },
  CLE: { primary: '#860038', secondary: '#041E42' },
  NOP: { primary: '#0C2340', secondary: '#C8102E' },
  CHI: { primary: '#CE1141', secondary: '#000000' },
  DAL: { primary: '#00538C', secondary: '#002B5E' },
  DEN: { primary: '#0E2240', secondary: '#FEC524' },
  GSW: { primary: '#1D428A', secondary: '#FFC72C' },
  HOU: { primary: '#CE1141', secondary: '#000000' },
  LAC: { primary: '#C8102E', secondary: '#1D428A' },
  LAL: { primary: '#552583', secondary: '#FDB927' },
  MIA: { primary: '#98002E', secondary: '#F9A01B' },
  MIL: { primary: '#00471B', secondary: '#0077C0' },
  MIN: { primary: '#0C2340', secondary: '#236192' },
  BKN: { primary: '#000000', secondary: '#777D84' },
  NYK: { primary: '#006BB6', secondary: '#F58426' },
  ORL: { primary: '#0077C0', secondary: '#000000' },
  IND: { primary: '#002D62', secondary: '#FDBB30' },
  PHI: { primary: '#006BB6', secondary: '#ED174C' },
  PHX: { primary: '#1D1160', secondary: '#E56020' },
  POR: { primary: '#E03A3E', secondary: '#000000' },
  SAC: { primary: '#5A2D81', secondary: '#63727A' },
  SAS: { primary: '#000000', secondary: '#8A8D8F' },
  OKC: { primary: '#007AC1', secondary: '#EF3B24' },
  TOR: { primary: '#CE1141', secondary: '#000000' },
  UTA: { primary: '#002B5C', secondary: '#F9A01B' },
  MEM: { primary: '#5D76A9', secondary: '#12173F' },
  WAS: { primary: '#002B5C', secondary: '#E31837' },
  DET: { primary: '#C8102E', secondary: '#1D42BA' },
  CHA: { primary: '#1D1160', secondary: '#00788C' },
};

/** Couleur d'une équipe inconnue (franchise disparue, données incomplètes). */
const NEUTRAL = '#6B7280';

/** En dessous de cette distance (RVB), deux couleurs se confondent sur une barre. */
const MIN_COLOR_DISTANCE = 80;

function branding(team: Team): Branding | undefined {
  return team?.abbreviation ? BRANDING[team.abbreviation.toUpperCase()] : undefined;
}

export function teamColor(team: Team): string {
  return branding(team)?.primary ?? NEUTRAL;
}

/**
 * Couleurs des deux équipes d'un match. Si leurs couleurs principales se ressemblent (Bulls et Raptors, Knicks et
 * 76ers…), l'équipe à l'extérieur prend sa couleur secondaire.
 */
export function matchupColors(home: Team, away: Team): { home: string; away: string } {
  const homeColor = teamColor(home);
  const awayColor = teamColor(away);
  if (distance(homeColor, awayColor) >= MIN_COLOR_DISTANCE) {
    return { home: homeColor, away: awayColor };
  }
  const secondary = branding(away)?.secondary ?? NEUTRAL;
  return { home: homeColor, away: distance(homeColor, secondary) >= MIN_COLOR_DISTANCE ? secondary : NEUTRAL };
}

/** Texte lisible sur un fond de cette couleur : blanc sur les couleurs sombres, presque noir sur les claires. */
export function textOn(background: string): string {
  const [r, g, b] = rgb(background).map(channel => {
    const c = channel / 255;
    return c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
  });
  const luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b;
  return luminance > 0.4 ? '#111827' : '#FFFFFF';
}

/**
 * Logo de l'équipe : l'adresse enregistrée en base (le site de la NBA, voir la migration 015), chargée par le
 * navigateur. {@code null} sans adresse : la page affiche alors une pastille avec l'abréviation.
 */
export function teamLogoUrl(team: Team): string | null {
  return team?.logoUrl || null;
}

function rgb(hex: string): [number, number, number] {
  const value = Number.parseInt(hex.replace('#', ''), 16);
  return [(value >> 16) & 255, (value >> 8) & 255, value & 255];
}

function distance(a: string, b: string): number {
  const [r1, g1, b1] = rgb(a);
  const [r2, g2, b2] = rgb(b);
  return Math.hypot(r1 - r2, g1 - g2, b1 - b2);
}
