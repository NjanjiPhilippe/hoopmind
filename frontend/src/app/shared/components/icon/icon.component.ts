import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Icônes disponibles, toutes dessinées au trait (contours) dans un repère 24 × 24. */
export type IconName = 'arrow-left' | 'trend' | 'scale' | 'bolt' | 'users' | 'database' | 'target' | 'folder';

/**
 * Icône au trait, couleur héritée (`currentColor`). Décorative : le texte voisin porte toujours le sens.
 * Remplace les émojis, dont le rendu change d'un système à l'autre.
 */
@Component({
  selector: 'app-icon',
  template: `
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"
         stroke-linejoin="round" aria-hidden="true" focusable="false" [attr.width]="size()" [attr.height]="size()">
      @switch (name()) {
        @case ('arrow-left') {
          <path d="M19 12H5M11 6l-6 6 6 6" />
        }
        @case ('trend') {
          <path d="M3 3v18h18" />
          <path d="M7 15l4-4 3 3 6-6" />
          <path d="M15 8h5v5" />
        }
        @case ('scale') {
          <path d="M12 4v16M8 20h8M5 7h14" />
          <path d="M5 7l-3 6a3 3 0 0 0 6 0L5 7zM19 7l-3 6a3 3 0 0 0 6 0l-3-6z" />
        }
        @case ('bolt') {
          <path d="M13 2L4 14h7l-1 8 9-12h-7l1-8z" />
        }
        @case ('users') {
          <circle cx="9" cy="8" r="3.5" />
          <path d="M2.5 20a6.5 6.5 0 0 1 13 0" />
          <path d="M16 4.6a3.5 3.5 0 0 1 0 6.8M18.5 14.2A6.5 6.5 0 0 1 21.5 20" />
        }
        @case ('database') {
          <ellipse cx="12" cy="5.5" rx="8" ry="3" />
          <path d="M4 5.5v13c0 1.7 3.6 3 8 3s8-1.3 8-3v-13" />
          <path d="M4 12c0 1.7 3.6 3 8 3s8-1.3 8-3" />
        }
        @case ('target') {
          <circle cx="12" cy="12" r="9" />
          <circle cx="12" cy="12" r="5" />
          <circle cx="12" cy="12" r="1" />
        }
        @case ('folder') {
          <path d="M3 6.5A1.5 1.5 0 0 1 4.5 5h4.3l2 2.5h8.7A1.5 1.5 0 0 1 21 9v9.5a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 18.5z" />
        }
      }
    </svg>
  `,
  host: { class: 'inline-flex shrink-0' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class IconComponent {
  readonly name = input.required<IconName>();
  readonly size = input(24);
}
