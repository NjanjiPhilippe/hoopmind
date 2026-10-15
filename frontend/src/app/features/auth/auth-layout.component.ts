import { ChangeDetectionStrategy, Component, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { IconComponent } from '../../shared/components/icon/icon.component';
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
  imports: [RouterLink, IconComponent, LogoComponent],
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

        <a routerLink="/" class="relative flex w-fit items-center gap-3">
          <app-logo [size]="40" />
          <span class="text-2xl font-bold">HoopMind</span>
        </a>
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

      <main class="flex flex-1 flex-col items-center justify-center px-4 py-12">
        <div class="w-full max-w-md">
          <a routerLink="/" class="mb-4 inline-flex items-center gap-2 text-sm font-medium text-neutral-300 hover:text-white lg:text-gray-600 lg:hover:text-gray-900">
            <app-icon name="arrow-left" [size]="18" /> Retour à l'accueil
          </a>
        </div>
        <div class="w-full max-w-md rounded-2xl bg-white p-8 shadow-2xl lg:shadow-xl">
          <div class="mb-8 text-center">
            <a routerLink="/" class="mx-auto mb-4 flex w-fit" aria-label="Accueil HoopMind">
              <app-logo [size]="56" />
            </a>
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
