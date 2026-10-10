import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, from, switchMap, throwError } from 'rxjs';

import { AuthService, SKIP_AUTH_REFRESH } from './auth.service';

/**
 * Ajoute le jeton d'accès aux appels de l'API. Sur une réponse 401, renouvelle la session une fois
 * puis rejoue la requête ; si le renouvellement échoue, renvoie vers la page de connexion.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  if (!isApiRequest(request)) {
    return next(request);
  }
  const auth = inject(AuthService);
  const router = inject(Router);

  return next(withToken(request, auth.token())).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401 || request.context.get(SKIP_AUTH_REFRESH)
          || !auth.token()) {
        return throwError(() => error);
      }
      return from(auth.refresh()).pipe(
        switchMap(refreshed => {
          if (!refreshed) {
            void router.navigate(['/auth/login'], { queryParams: { returnUrl: router.url } });
            return throwError(() => error);
          }
          return next(withToken(request, auth.token()));
        }),
      );
    }),
  );
};

function isApiRequest(request: HttpRequest<unknown>): boolean {
  return request.url.startsWith('/api/') || request.url.includes('/api/v1/');
}

function withToken(request: HttpRequest<unknown>, token: string | null): HttpRequest<unknown> {
  return token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request;
}
