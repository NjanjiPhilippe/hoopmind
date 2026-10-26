import { PlayerCreateRequest } from '../../../core/api/generated/models/player-create-request';
import { PlayerResponse } from '../../../core/api/generated/models/player-response';
import { PlayerSeasonStatsRequest } from '../../../core/api/generated/models/player-season-stats-request';
import { PlayerSeasonStatsResponse } from '../../../core/api/generated/models/player-season-stats-response';

export type Position = PlayerCreateRequest['position'];
export type PlayerStatus = NonNullable<PlayerResponse['status']>;

export const POSITIONS: { value: Position; label: string }[] = [
  { value: 'POINT_GUARD', label: 'Meneur' },
  { value: 'SHOOTING_GUARD', label: 'Arrière' },
  { value: 'SMALL_FORWARD', label: 'Ailier' },
  { value: 'POWER_FORWARD', label: 'Ailier fort' },
  { value: 'CENTER', label: 'Pivot' },
];

export const STATUSES: { value: PlayerStatus; label: string }[] = [
  { value: 'ACTIVE', label: 'Actif' },
  { value: 'INJURED', label: 'Blessé' },
  { value: 'SUSPENDED', label: 'Suspendu' },
  { value: 'INACTIVE', label: 'Inactif' },
];

export function positionLabel(position: Position | undefined): string {
  return POSITIONS.find(p => p.value === position)?.label ?? '';
}

export function statusLabel(status: PlayerStatus | undefined): string {
  return STATUSES.find(s => s.value === status)?.label ?? '';
}

export type PlayerForm = Omit<PlayerCreateRequest, 'teamId'> & { status: PlayerStatus };

/** Lit le formulaire d'un joueur. Taille en centimètres, poids en kilogrammes, numéro de 0 à 99. */
export function parsePlayer(values: Record<string, string>): PlayerForm | { error: string } {
  const firstName = values['firstName']?.trim() ?? '';
  const lastName = values['lastName']?.trim() ?? '';
  if (!firstName || !lastName) {
    return { error: 'Le prénom et le nom sont obligatoires.' };
  }
  const jerseyNumber = optionalNumber(values['jerseyNumber']);
  if (jerseyNumber !== undefined && (!Number.isInteger(jerseyNumber) || jerseyNumber < 0 || jerseyNumber > 99)) {
    return { error: 'Le numéro va de 0 à 99.' };
  }
  const height = optionalNumber(values['height']);
  const weight = optionalNumber(values['weight']);
  if ((height !== undefined && !(height > 0 && height <= 260)) || (weight !== undefined && !(weight > 0 && weight <= 200))) {
    return { error: 'Taille en centimètres (jusqu\'à 260), poids en kilogrammes (jusqu\'à 200).' };
  }
  if (Number.isNaN(jerseyNumber) || Number.isNaN(height) || Number.isNaN(weight)) {
    return { error: 'Numéro, taille et poids doivent être des nombres.' };
  }
  return {
    firstName,
    lastName,
    position: (values['position'] as Position) || 'POINT_GUARD',
    status: (values['status'] as PlayerStatus) || 'ACTIVE',
    jerseyNumber,
    height,
    weight,
  };
}

const PER_GAME: { key: keyof PlayerSeasonStatsRequest; label: string; max: number }[] = [
  { key: 'minutesPerGame', label: 'Minutes', max: 60 },
  { key: 'pointsPerGame', label: 'Points', max: 100 },
  { key: 'totalReboundsPerGame', label: 'Rebonds', max: 40 },
  { key: 'assistsPerGame', label: 'Passes', max: 30 },
  { key: 'stealsPerGame', label: 'Interceptions', max: 15 },
  { key: 'blocksPerGame', label: 'Contres', max: 15 },
  { key: 'turnoversPerGame', label: 'Balles perdues', max: 15 },
];

const PERCENTAGES: { key: keyof PlayerSeasonStatsRequest; label: string }[] = [
  { key: 'fieldGoalPercentage', label: 'Tirs (%)' },
  { key: 'threePointPercentage', label: '3 points (%)' },
  { key: 'freeThrowPercentage', label: 'Lancers francs (%)' },
];

/** Les champs du formulaire de statistiques, dans l'ordre d'affichage. */
export const STAT_FIELDS = [
  ...PER_GAME.map(f => ({ key: f.key, label: f.label + ' / match', percent: false })),
  ...PERCENTAGES.map(f => ({ key: f.key, label: f.label, percent: true })),
];

/**
 * Lit le formulaire de statistiques. Les pourcentages se saisissent comme on les lit (47,5) et partent au serveur entre
 * 0 et 1 (0,475) ; la virgule décimale est acceptée.
 */
export function parseStats(values: Record<string, string>): PlayerSeasonStatsRequest | { error: string } {
  const gamesPlayed = optionalNumber(values['gamesPlayed']);
  const gamesStarted = optionalNumber(values['gamesStarted']);
  if (gamesPlayed === undefined || !Number.isInteger(gamesPlayed) || gamesPlayed < 0 || gamesPlayed > 110) {
    return { error: 'Indiquez le nombre de matchs joués (0 à 110).' };
  }
  if (gamesStarted !== undefined && (!Number.isInteger(gamesStarted) || gamesStarted < 0 || gamesStarted > gamesPlayed)) {
    return { error: 'Les titularisations vont de 0 au nombre de matchs joués.' };
  }
  const body: Record<string, number | undefined> = { gamesPlayed, gamesStarted };
  for (const field of PER_GAME) {
    const value = optionalNumber(values[field.key]);
    if (value !== undefined && !(value >= 0 && value <= field.max)) {
      return { error: `${field.label} par match : entre 0 et ${field.max}.` };
    }
    body[field.key] = value;
  }
  for (const field of PERCENTAGES) {
    const value = optionalNumber(values[field.key]);
    if (value !== undefined && !(value >= 0 && value <= 100)) {
      return { error: `${field.label} : entre 0 et 100.` };
    }
    body[field.key] = value === undefined ? undefined : Math.round(value * 10) / 1000;
  }
  return { ...body, gamesPlayed };
}

/** Valeur à afficher dans le formulaire pour une ligne déjà saisie (pourcentages remis sur 100). */
export function statValue(stats: PlayerSeasonStatsResponse | null | undefined, key: string, percent: boolean): string {
  const value = stats?.[key as keyof PlayerSeasonStatsResponse];
  if (typeof value !== 'number') {
    return '';
  }
  return percent ? String(Math.round(value * 1000) / 10) : String(value);
}

function optionalNumber(raw: string | undefined): number | undefined {
  const text = raw?.trim().replace(',', '.');
  return text ? Number(text) : undefined;
}
