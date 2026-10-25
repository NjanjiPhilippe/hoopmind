import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * Logo HoopMind : les seules coutures d'un ballon de basket, tracées à main levée (traits volontairement
 * irréguliers, qui débordent un peu). Couleur héritée (`currentColor`), orange par défaut. Décoratif : le nom
 * de l'application est toujours écrit à côté. Même dessin que `public/favicon.svg`.
 */
@Component({
  selector: 'app-logo',
  template: `
    <svg viewBox="0 0 48 48" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round"
         aria-hidden="true" focusable="false" [attr.width]="size()" [attr.height]="size()" [attr.stroke-width]="weight()">
      <path d="M25.1 4.9C35.6 5.2 43.5 13.9 43 24.6 42.6 34.9 33.8 43.4 23.3 42.9 13 42.4 4.6 33.7 5.2 23.2 5.7 13.1 14 5.3 22.4 5.8" />
      <path d="M23.6 4.6C24.9 15.8 23 30.7 24.4 43.3" />
      <path d="M4.9 24.4C16.5 22.6 31.2 25.6 43.4 23.4" />
      <path d="M10.4 11.2C19.9 16.1 19.6 31.4 9.8 37.3" />
      <path d="M37.9 10.6C28.3 17.4 28.9 30.2 38.6 36.4" />
    </svg>
  `,
  host: { class: 'inline-flex shrink-0 text-primary-500' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LogoComponent {
  readonly size = input(36);
  /** Épaisseur du trait, dans le repère 48 × 48 : plus épais pour les petites tailles. */
  readonly weight = input(3.2);
}
