import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import localeFr from '@angular/common/locales/fr';
import {
  ApplicationConfig,
  LOCALE_ID,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';

import { routes } from './app.routes';
import { provideApiConfiguration } from './core/api/generated/api-configuration';
import { authInterceptor } from './core/auth/auth.interceptor';
import { AuthService } from './core/auth/auth.service';

registerLocaleData(localeFr);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    { provide: LOCALE_ID, useValue: 'fr' },
    // Zone.js reste actif tant que les pages NBA existantes n'utilisent pas les signals ;
    // le nouveau code (auth, compte, admin) est déjà OnPush + signals, prêt pour le mode zoneless.
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withFetch(), withInterceptors([authInterceptor])),
    // Chemins relatifs : en développement le proxy d'Angular redirige /api vers le backend,
    // en production le serveur web fait de même (voir nginx.conf).
    provideApiConfiguration(''),
    // Rouvre la session à partir du cookie de rafraîchissement avant la première navigation.
    provideAppInitializer(() => inject(AuthService).restoreSession()),
  ],
};
