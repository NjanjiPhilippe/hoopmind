import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { provideApiConfiguration } from '../api/generated/api-configuration';
import { AuthService } from './auth.service';
import { authResponse } from './testing/auth-fixtures';

describe('AuthService', () => {
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([{ path: '**', children: [] }]), provideApiConfiguration('')],
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('ouvre une session à la connexion et garde le jeton en mémoire', async () => {
    const login = auth.login({ login: 'saiko', password: 'Password123' });
    http.expectOne({ method: 'POST', url: '/api/v1/auth/login' }).flush(authResponse());

    await login;

    expect(auth.isAuthenticated()).toBe(true);
    expect(auth.token()).toBe('access-token');
    expect(auth.isAdmin()).toBe(false);
    expect(localStorage.length).toBe(0);
  });

  it('reconnaît un administrateur', async () => {
    const login = auth.login({ login: 'admin', password: 'x' });
    http.expectOne('/api/v1/auth/login').flush(authResponse(['ROLE_USER', 'ROLE_ADMIN']));
    await login;

    expect(auth.isAdmin()).toBe(true);
  });

  it('partage une seule requête entre des rafraîchissements simultanés', async () => {
    const first = auth.refresh();
    const second = auth.refresh();

    http.expectOne({ method: 'POST', url: '/api/v1/auth/refresh' }).flush(authResponse());

    expect(await first).toBe(true);
    expect(await second).toBe(true);
    expect(auth.user()?.username).toBe('saiko');
  });

  it('ferme la session si le rafraîchissement échoue', async () => {
    const refresh = auth.restoreSession();
    http.expectOne('/api/v1/auth/refresh').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(await refresh).toBe(false);
    expect(auth.isAuthenticated()).toBe(false);
    expect(auth.token()).toBeNull();
  });

  it('ferme la session locale même si la déconnexion côté serveur échoue', async () => {
    const login = auth.login({ login: 'saiko', password: 'x' });
    http.expectOne('/api/v1/auth/login').flush(authResponse());
    await login;

    const logout = auth.logout();
    http.expectOne('/api/v1/auth/logout').flush(null, { status: 0, statusText: 'Network error' });
    await logout;

    expect(auth.isAuthenticated()).toBe(false);
  });
});
