import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from './auth.service';

/** Page réservée aux utilisateurs connectés. */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  return auth.isAuthenticated()
    || inject(Router).createUrlTree(['/auth/login'], { queryParams: { returnUrl: state.url } });
};

/** Page réservée aux administrateurs ; un utilisateur connecté sans ce rôle voit la page « Accès refusé ». */
export const adminGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.isAuthenticated()) {
    return router.createUrlTree(['/auth/login'], { queryParams: { returnUrl: state.url } });
  }
  return auth.isAdmin() || router.createUrlTree(['/forbidden']);
};

/** Pages de connexion et d'inscription : inutiles une fois connecté. */
export const guestGuard: CanActivateFn = () => {
  return !inject(AuthService).isAuthenticated() || inject(Router).createUrlTree(['/dashboard']);
};
