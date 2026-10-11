import { AuditEvent } from '../../../core/api/generated/models/audit-event';

export type AuditAction = AuditEvent['action'];

export const AUDIT_ACTION_LABELS: Record<AuditAction, string> = {
  USER_REGISTERED: 'Inscription',
  LOGIN_SUCCEEDED: 'Connexion',
  LOGIN_FAILED: 'Échec de connexion',
  LOGIN_THROTTLED: 'Connexion bloquée (trop de tentatives)',
  LOGOUT: 'Déconnexion',
  REFRESH_TOKEN_REUSED: 'Jeton de session réutilisé',
  PROFILE_UPDATED: 'Profil modifié',
  PASSWORD_CHANGED: 'Mot de passe changé',
  PASSWORD_CHANGE_FAILED: 'Échec du changement de mot de passe',
  ADMIN_BOOTSTRAPPED: 'Administrateur initial créé',
  USER_ENABLED: 'Compte réactivé',
  USER_DISABLED: 'Compte désactivé',
  USER_ROLES_CHANGED: 'Rôles modifiés',
  USER_DELETED: 'Compte supprimé',
  DATA_IMPORTED: 'Import de données',
  MODEL_TRAINED: 'Entraînement du modèle',
};

export const AUDIT_ACTIONS = Object.keys(AUDIT_ACTION_LABELS) as AuditAction[];

const REASON_LABELS: Record<string, string> = {
  UNKNOWN_ACCOUNT: 'identifiant inconnu',
  BAD_PASSWORD: 'mot de passe incorrect',
  ACCOUNT_DISABLED: 'compte désactivé',
  WRONG_CURRENT_PASSWORD: 'mot de passe actuel incorrect',
  LAST_ADMIN: 'dernier administrateur actif',
  TOKEN_ALREADY_USED: 'jeton déjà utilisé, sessions révoquées',
  TOO_MANY_ATTEMPTS: 'trop de tentatives récentes',
  IMPORT_FAILED: "échec de l'import",
  TRAINING_FAILED: "échec de l'entraînement",
};

const FIELD_LABELS: Record<string, string> = {
  firstName: 'prénom',
  lastName: 'nom',
  bio: 'bio',
  favoriteTeamId: 'équipe favorite',
};

const ROLE_LABELS: Record<string, string> = {
  ROLE_USER: 'utilisateur',
  ROLE_ADMIN: 'administrateur',
};

function list(value: string, labels: Record<string, string>): string {
  return value.split(',').filter(Boolean).map(v => labels[v] ?? v).join(', ');
}

/** Résumé lisible des détails d'un événement, par exemple « Motif : mot de passe incorrect ». */
export function describeDetails(event: Pick<AuditEvent, 'details'>): string {
  const { reason, fields, before, after, revokedTokens, ...rest } = event.details;
  const parts: string[] = [];
  if (reason) parts.push(`Motif : ${REASON_LABELS[reason] ?? reason}`);
  if (fields) parts.push(`Champs : ${list(fields, FIELD_LABELS)}`);
  if (before !== undefined || after !== undefined) {
    parts.push(`Rôles : ${list(before ?? '', ROLE_LABELS) || '—'} → ${list(after ?? '', ROLE_LABELS) || '—'}`);
  }
  if (revokedTokens) parts.push(`${revokedTokens} jeton(s) révoqué(s)`);
  for (const [key, value] of Object.entries(rest)) parts.push(`${key} : ${value}`);
  return parts.join(' · ');
}
