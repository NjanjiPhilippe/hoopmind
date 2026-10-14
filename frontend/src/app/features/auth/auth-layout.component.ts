import { ChangeDetectionStrategy, Component, input, signal } from '@angular/core';

import { LogoComponent } from '../../shared/components/logo/logo.component';

/** Photo du panneau gauche : Kobe Bryant au dunk, chargée depuis Wikimedia Commons (aucune copie dans le dépôt). */
const HERO_PHOTO = {
  src: 'https://commons.wikimedia.org/wiki/Special:FilePath/Kobe_Bryant_dunk.jpg?width=1600',
  page: 'https://commons.wikimedia.org/wiki/File:Kobe_Bryant_dunk.jpg',
};

/**
 * Cadre commun des pages de connexion et d'inscription : panneau visuel à gauche (fond noir), carte à droite.
 * Le panneau affiche la photo `HERO_PHOTO`, très transparente sur le noir ; si elle ne charge pas (hors ligne,
 * lien cassé), le panneau reste noir. Sur petit écran, seule la carte reste, sur fond noir.
 */
@Component({
  selector: 'app-auth-layout',
  imports: [LogoComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex min-h-screen bg-neutral-950 lg:bg-gray-50">
      <aside class="relative hidden w-1/2 flex-col justify-between overflow-hidden bg-neutral-950 p-12 text-white lg:flex xl:w-[55%]">
        @if (!photoMissing()) {
          <img [src]="photo.src" alt="" aria-hidden="true" referrerpolicy="no-referrer"
               class="absolute inset-0 h-full w-full object-cover object-top opacity-35 grayscale-[30%]"
               (error)="photoMissing.set(true)" />
          <div class="absolute inset-0 bg-gradient-to-t from-neutral-950 via-neutral-950/40 to-neutral-950/70"></div>
        }

        <div class="relative flex items-center gap-3">
          <app-logo [size]="40" />
          <span class="text-2xl font-bold">HoopMind</span>
        </div>
        <div class="relative max-w-md">
          <p class="text-3xl font-bold leading-tight">Prédire les matchs NBA, et savoir à quel point s'y fier.</p>
          <p class="mt-3 text-neutral-400">Un modèle Spark ML évalué face à des règles simples, expliqué en langage clair.</p>
          @if (!photoMissing()) {
            <p class="mt-6 text-xs text-neutral-500">
              Photo : <a [href]="photo.page" target="_blank" rel="noopener" class="underline hover:text-neutral-300">Wikimedia Commons</a>
            </p>
          }
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

  protected readonly photo = HERO_PHOTO;
  protected readonly photoMissing = signal(false);
}
