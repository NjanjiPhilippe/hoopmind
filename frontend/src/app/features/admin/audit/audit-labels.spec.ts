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
