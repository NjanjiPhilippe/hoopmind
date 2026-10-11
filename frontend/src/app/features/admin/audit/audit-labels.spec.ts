import { describe, expect, it } from 'vitest';

import { AUDIT_ACTIONS, AUDIT_ACTION_LABELS, describeDetails } from './audit-labels';

describe('describeDetails', () => {
  it('traduit le motif d’un échec', () => {
    expect(describeDetails({ details: { reason: 'BAD_PASSWORD' } })).toBe('Motif : mot de passe incorrect');
  });

  it('liste les champs modifiés et les changements de rôles', () => {
    expect(describeDetails({ details: { fields: 'firstName,bio' } })).toBe('Champs : prénom, bio');
    expect(describeDetails({ details: { before: 'ROLE_USER', after: 'ROLE_ADMIN,ROLE_USER' } }))
      .toBe('Rôles : utilisateur → administrateur, utilisateur');
  });

  it('résume un import et un entraînement', () => {
    expect(describeDetails({ details: { downloaded: 'true', seasons: '2022-23,2023-24,2024-25', games: '3690' } }))
      .toBe('Téléchargée depuis Kaggle · Saisons 2022-23 à 2024-25 (3) · 3\u202f690 matchs');
    expect(describeDetails({ details: { reason: 'IMPORT_FAILED', download: 'false', seasons: '5', error: 'Base introuvable' } }))
      .toBe("Motif : échec de l'import · 5 saison(s) demandée(s) · Erreur : Base introuvable");
    expect(describeDetails({ details: { files: '3', importedFiles: '2', failedFiles: '1', insertedRows: '120', updatedRows: '4' } }))
      .toBe('Fichiers : 2 importé(s) sur 3, 1 en erreur · Lignes : 120 nouvelle(s), 4 mise(s) à jour');
    expect(describeDetails({ details: { accuracy: '0.583', testSeason: '2024-2025' } }))
      .toBe('Précision : 58,3 % · testé sur 2024-2025');
    expect(describeDetails({ details: { retryAfterSeconds: '30' } })).toBe('Bloquée 30 s');
  });

  it('garde les valeurs inconnues telles quelles', () => {
    expect(describeDetails({ details: { reason: 'NEW_REASON', source: 'import' } }))
      .toBe('Motif : NEW_REASON · source : import');
    expect(describeDetails({ details: {} })).toBe('');
  });
});

describe('AUDIT_ACTIONS', () => {
  it('a un libellé pour chaque action', () => {
    expect(AUDIT_ACTIONS.length).toBeGreaterThan(10);
    expect(AUDIT_ACTIONS.every(action => AUDIT_ACTION_LABELS[action].length > 0)).toBe(true);
  });
});
