import { HttpClient, HttpContext, HttpContextToken } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { ApiConfiguration } from '../api/generated/api-configuration';
import { login } from '../api/generated/fn/authentification/login';
import { logout } from '../api/generated/fn/authentification/logout';
import { refreshSession } from '../api/generated/fn/authentification/refresh-session';
import { register } from '../api/generated/fn/authentification/register';
import { AuthResponse } from '../api/generated/models/auth-response';
import { LoginRequest } from '../api/generated/models/login-request';
import { RegisterRequest } from '../api/generated/models/register-request';
import { UserProfile } from '../api/generated/models/user-profile';

/** Marque les appels d'authentification pour que l'intercepteur ne tente pas de les rafraîchir. */
export const SKIP_AUTH_REFRESH = new HttpContextToken<boolean>(() => false);

export type Role = UserProfile['roles'][number];

/**
 * État de la session.
 *
 * Le jeton d'accès reste en mémoire (jamais dans le localStorage, hors de portée d'un script injecté).
 * Le jeton de rafraîchissement est un cookie HttpOnly posé par le backend : au chargement de l'application,
 * {@link restoreSession} l'échange contre un nouveau jeton d'accès.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly rootUrl = inject(ApiConfiguration).rootUrl;

  private readonly accessToken = signal<string | null>(null);
  private readonly _user = signal<UserProfile | null>(null);
  private refreshInFlight: Promise<boolean> | null = null;

  readonly user = this._user.asReadonly();
  readonly isAuthenticated = computed(() => this._user() !== null);
  readonly isAdmin = computed(() => this.hasRole('ROLE_ADMIN'));

  token(): string | null {
    return this.accessToken();
  }

  hasRole(role: Role): boolean {
    return this._user()?.roles.includes(role) ?? false;
  }

  async login(credentials: LoginRequest): Promise<UserProfile> {
    const response = await firstValueFrom(login(this.http, this.rootUrl, { body: credentials }, this.authContext()));
    return this.openSession(response.body);
  }

  async register(data: RegisterRequest): Promise<UserProfile> {
    const response = await firstValueFrom(register(this.http, this.rootUrl, { body: data }, this.authContext()));
    return this.openSession(response.body);
  }

  /** Ouvre la session à partir du cookie de rafraîchissement, s'il existe. Ne lève jamais d'erreur. */
  restoreSession(): Promise<boolean> {
    return this.refresh();
  }

  /**
   * Renouvelle le jeton d'accès. Les appels simultanés partagent la même requête : le backend fait tourner
   * le jeton de rafraîchissement et rejetterait le second échange comme une réutilisation.
   */
  refresh(): Promise<boolean> {
    this.refreshInFlight ??= firstValueFrom(refreshSession(this.http, this.rootUrl, undefined, this.authContext()))
      .then(response => {
        this.openSession(response.body);
        return true;
      })
      .catch(() => {
        this.clearSession();
        return false;
      })
      .finally(() => (this.refreshInFlight = null));
    return this.refreshInFlight;
  }

  async logout(): Promise<void> {
    try {
      await firstValueFrom(logout(this.http, this.rootUrl, undefined, this.authContext()));
    } catch {
      // La session locale est fermée quoi qu'il arrive.
    }
    this.clearSession();
    await this.router.navigate(['/auth/login']);
  }

  /** Met à jour l'utilisateur affiché après une modification de profil. */
  updateUser(user: UserProfile): void {
    this._user.set(user);
  }

  /** Ferme la session côté client (jeton expiré et non renouvelable, compte désactivé...). */
  clearSession(): void {
    this.accessToken.set(null);
    this._user.set(null);
  }

  private openSession(response: AuthResponse): UserProfile {
    this.accessToken.set(response.accessToken);
    this._user.set(response.user);
    return response.user;
  }

  private authContext(): HttpContext {
    return new HttpContext().set(SKIP_AUTH_REFRESH, true);
  }
}
