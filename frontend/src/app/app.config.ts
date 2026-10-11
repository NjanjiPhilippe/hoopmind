import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import localeFr from '@angular/common/locales/fr';
import {
  ApplicationConfig,
  LOCALE_ID,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
  provideZonelessChangeDetection,
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
    // Sans zone.js : tous les composants sont OnPush et leur état est en signals, Angular sait seul quand rafraîchir.
    provideZonelessChangeDetection(),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withFetch(), withInterceptors([authInterceptor])),
    // Chemins relatifs : en développement le proxy d'Angular redirige /api vers le backend,
    // en production le serveur web fait de même (voir nginx.conf).
    provideApiConfiguration(''),
    // Rouvre la session à partir du cookie de rafraîchissement avant la première navigation.
    provideAppInitializer(() => inject(AuthService).restoreSession()),
  ],
};
