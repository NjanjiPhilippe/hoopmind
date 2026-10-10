import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';

import { provideApiConfiguration } from '../api/generated/api-configuration';
import { adminGuard, authGuard, guestGuard } from './auth.guards';
import { AuthService } from './auth.service';
import { authResponse } from './testing/auth-fixtures';

describe('auth guards', () => {
  const route = {} as ActivatedRouteSnapshot;
  const state = { url: '/account' } as RouterStateSnapshot;
  let http: HttpTestingController;

  const run = (guard: typeof authGuard) => TestBed.runInInjectionContext(() => guard(route, state));
  const loginAs = async (roles: Parameters<typeof authResponse>[0]) => {
    const login = TestBed.inject(AuthService).login({ login: 'x', password: 'x' });
    http.expectOne('/api/v1/auth/login').flush(authResponse(roles));
    await login;
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), provideApiConfiguration('')],
    });
    http = TestBed.inject(HttpTestingController);
  });

  it('redirige un visiteur vers la connexion en gardant la page demandée', () => {
    const result = run(authGuard) as UrlTree;

    expect(result.toString()).toBe('/auth/login?returnUrl=%2Faccount');
  });

  it('laisse passer un utilisateur connecté', async () => {
    await loginAs(['ROLE_USER']);

    expect(run(authGuard)).toBe(true);
  });

  it('refuse l\'administration à un simple utilisateur', async () => {
    await loginAs(['ROLE_USER']);

    expect((run(adminGuard) as UrlTree).toString()).toBe('/');
  });

  it('ouvre l\'administration à un administrateur', async () => {
    await loginAs(['ROLE_USER', 'ROLE_ADMIN']);

    expect(run(adminGuard)).toBe(true);
  });

  it('renvoie un utilisateur connecté hors des pages de connexion', async () => {
    await loginAs(['ROLE_USER']);

    expect((run(guestGuard) as UrlTree).toString()).toBe('/account');
  });
});
