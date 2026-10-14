import { ChangeDetectionStrategy, Component, input, signal } from '@angular/core';

import { LogoComponent } from '../../shared/components/logo/logo.component';
import { LoginIllustrationComponent } from './login-illustration.component';

/**
 * Cadre commun des pages de connexion et d'inscription : panneau visuel à gauche (fond noir), carte à droite.
 * Le panneau affiche `public/login-hero.jpg` si ce fichier existe en local (il n'est pas versionné : droits
 * d'image), sinon l'illustration vectorielle. Sur petit écran, seule la carte reste, sur fond noir.
 */
@Component({
  selector: 'app-auth-layout',
  imports: [LogoComponent, LoginIllustrationComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex min-h-screen bg-neutral-950 lg:bg-gray-50">
      <aside class="relative hidden w-1/2 flex-col justify-between overflow-hidden bg-neutral-950 p-12 text-white lg:flex xl:w-[55%]">
        @if (!photoMissing()) {
          <img src="login-hero.jpg" alt="" aria-hidden="true" class="absolute inset-0 h-full w-full object-cover opacity-80"
               (error)="photoMissing.set(true)" />
          <div class="absolute inset-0 bg-gradient-to-t from-neutral-950 via-neutral-950/30 to-neutral-950/60"></div>
        } @else {
          <app-login-illustration class="absolute inset-x-0 top-1/2 mx-auto h-[62%] max-h-[560px] -translate-y-1/2" />
        }

        <div class="relative flex items-center gap-3">
          <app-logo [size]="40" />
          <span class="text-2xl font-bold">HoopMind</span>
        </div>
        <div class="relative max-w-md">
          <p class="text-3xl font-bold leading-tight">Prédire les matchs NBA, et savoir à quel point s'y fier.</p>
          <p class="mt-3 text-neutral-400">Un modèle Spark ML évalué face à des règles simples, expliqué en langage clair.</p>
        </div>
      </aside>

      <main class="flex flex-1 items-center justify-center px-4 py-12">
        <div class="w-full max-w-md rounded-2xl bg-white p-8 shadow-2xl lg:shadow-xl">
          <div class="mb-8 text-center">
            <app-logo class="mx-auto mb-4 lg:hidden" [size]="56" />
            <h1 class="text-3xl font-bold text-gray-900">{{ heading() }}</h1>
            <p class="mt-2 text-gray-600">{{ subheading() }}</p>
          </div>
          <ng-content />
        </div>
      </main>
    </div>
  `,
})
export class AuthLayoutComponent {
  readonly heading = input.required<string>();
  readonly subheading = input('');

  protected readonly photoMissing = signal(false);
}
