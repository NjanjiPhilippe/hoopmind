import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';

/**
 * Pages « Accès refusé » (403) et « Page introuvable » (404). Elles expliquent ce qui s'est passé au lieu de renvoyer
 * en silence vers l'accueil, et proposent un chemin pour continuer.
 */
@Component({
  selector: 'app-error-page',
  imports: [RouterLink, NavbarComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="min-h-screen bg-gray-50">
      <app-navbar />
      <main class="mx-auto max-w-xl px-4 py-16 text-center">
        <p class="text-6xl font-bold text-primary-700">{{ kind() === 'forbidden' ? '403' : '404' }}</p>
        @if (kind() === 'forbidden') {
          <h1 class="mt-4 text-2xl font-bold text-gray-900">Accès refusé</h1>
          <p class="mt-2 text-gray-600">Cette page est réservée aux administrateurs. Si vous pensez devoir y accéder, demandez ce rôle à un administrateur.</p>
        } @else {
          <h1 class="mt-4 text-2xl font-bold text-gray-900">Page introuvable</h1>
          <p class="mt-2 text-gray-600">L'adresse ne correspond à aucune page de HoopMind. Elle a peut-être changé, ou contient une faute de frappe.</p>
        }
        <a [routerLink]="auth.isAuthenticated() ? '/dashboard' : '/'" class="btn-primary mt-8 inline-block">Revenir à l'accueil</a>
      </main>
    </div>
  `,
})
export class ErrorPageComponent {
  protected readonly auth = inject(AuthService);
  /** Fourni par la route (data). */
  readonly kind = input<'forbidden' | 'not-found'>('not-found');
}
