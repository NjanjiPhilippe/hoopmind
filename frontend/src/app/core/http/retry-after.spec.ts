import { HttpErrorResponse, HttpHeaders } from '@angular/common/http';

import { retryAfterSeconds } from './retry-after';

describe('retryAfterSeconds', () => {
  it('reads the delay from the problem body first', () => {
    const error = new HttpErrorResponse({ status: 429, error: { retryAfterSeconds: 30 }, headers: new HttpHeaders({ 'Retry-After': '10' }) });
    expect(retryAfterSeconds(error)).toBe(30);
  });

  it('falls back on the Retry-After header', () => {
    expect(retryAfterSeconds(new HttpErrorResponse({ status: 429, headers: new HttpHeaders({ 'Retry-After': '12' }) }))).toBe(12);
  });

  it('ignores other errors', () => {
    expect(retryAfterSeconds(new HttpErrorResponse({ status: 401, error: { retryAfterSeconds: 30 } }))).toBeNull();
    expect(retryAfterSeconds(new HttpErrorResponse({ status: 429 }))).toBeNull();
    expect(retryAfterSeconds(new Error('boom'))).toBeNull();
  });
});
