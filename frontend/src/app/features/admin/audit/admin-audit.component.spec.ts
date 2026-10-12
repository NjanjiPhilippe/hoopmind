import { filtersFromUrl } from './admin-audit.component';

describe('filtersFromUrl', () => {
  it('reads the filters of a shared link', () => {
    expect(filtersFromUrl({ user: ' saiko ', action: 'LOGIN_FAILED', outcome: 'FAILURE', from: '2026-10-01', to: '2026-10-11' }))
      .toEqual({ user: 'saiko', action: 'LOGIN_FAILED', outcome: 'FAILURE', from: '2026-10-01', to: '2026-10-11' });
  });

  it('ignores values edited by hand that the API would reject', () => {
    expect(filtersFromUrl({ action: 'DROP_TABLE', outcome: 'MAYBE', from: 'hier', to: '11/10/2026' }))
      .toEqual({ user: '', action: '', outcome: '', from: '', to: '' });
  });
});
