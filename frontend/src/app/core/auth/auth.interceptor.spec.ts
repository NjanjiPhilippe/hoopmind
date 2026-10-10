import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { provideApiConfiguration } from '../api/generated/api-configuration';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';
import { authResponse } from './testing/auth-fixtures';

describe('authInterceptor', () => {
  let client: HttpClient;
  let http: HttpTestingController;
  let auth: AuthService;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        provideApiConfiguration(''),
      ],
    });
    client = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);

    const login = auth.login({ login: 'saiko', password: 'x' });
    http.expectOne('/api/v1/auth/login').flush(authResponse(['ROLE_USER'], 'old-token'));
    await login;
  });

  afterEach(() => http.verify());

  it('ajoute le jeton aux appels de l\'API', () => {
    client.get('/api/v1/users/me').subscribe();

    expect(http.expectOne('/api/v1/users/me').request.headers.get('Authorization')).toBe('Bearer old-token');
  });

  it('n\'envoie pas le jeton à un autre domaine', () => {
    client.get('https://example.com/data').subscribe();

    expect(http.expectOne('https://example.com/data').request.headers.has('Authorization')).toBe(false);
  });

  it('renouvelle la session sur un 401 puis rejoue la requête', async () => {
    const result = firstValueFrom(client.get<{ ok: boolean }>('/api/v1/users/me'));

    http.expectOne('/api/v1/users/me').flush(null, { status: 401, statusText: 'Unauthorized' });
    await Promise.resolve();
    http.expectOne('/api/v1/auth/refresh').flush(authResponse(['ROLE_USER'], 'new-token'));
    await vi.waitFor(() => http.expectOne(req => req.url === '/api/v1/users/me'
        && req.headers.get('Authorization') === 'Bearer new-token').flush({ ok: true }));

    expect(await result).toEqual({ ok: true });
  });

  it('renvoie vers la connexion si le renouvellement échoue', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const result = firstValueFrom(client.get('/api/v1/users/me'));

    http.expectOne('/api/v1/users/me').flush(null, { status: 401, statusText: 'Unauthorized' });
    await Promise.resolve();
    http.expectOne('/api/v1/auth/refresh').flush(null, { status: 401, statusText: 'Unauthorized' });

    await expect(result).rejects.toMatchObject({ status: 401 });
    expect(navigate).toHaveBeenCalledWith(['/auth/login'], expect.anything());
    expect(auth.isAuthenticated()).toBe(false);
  });
});
