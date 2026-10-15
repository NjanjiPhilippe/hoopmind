import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Icônes disponibles, toutes dessinées au trait (contours) dans un repère 24 × 24. */
export type IconName = 'eye' | 'eye-off' | 'trend' | 'scale' | 'bolt' | 'users' | 'database' | 'target' | 'folder' | 'calendar' | 'trophy' | 'chat';

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
        @case ('eye') {
          <path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12z" />
          <circle cx="12" cy="12" r="3" />
        }
        @case ('eye-off') {
          <path d="M10.6 5.1A10 10 0 0 1 12 5c6.4 0 10 7 10 7a17 17 0 0 1-2.6 3.4M6.6 6.6C3.7 8.4 2 12 2 12s3.6 7 10 7a9.6 9.6 0 0 0 5.4-1.6" />
          <path d="M9.9 9.9a3 3 0 0 0 4.2 4.2M3 3l18 18" />
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
        @case ('calendar') {
          <rect x="3.5" y="5" width="17" height="15.5" rx="2" />
          <path d="M3.5 10h17M8 3v4M16 3v4" />
        }
        @case ('trophy') {
          <path d="M8 4h8v5a4 4 0 0 1-8 0V4z" />
          <path d="M8 6H5a3 3 0 0 0 3 4M16 6h3a3 3 0 0 1-3 4M12 13v4M8.5 20h7M10 17h4" />
        }
        @case ('chat') {
          <path d="M4 5.5A1.5 1.5 0 0 1 5.5 4h13A1.5 1.5 0 0 1 20 5.5v9a1.5 1.5 0 0 1-1.5 1.5H10l-4.5 4v-4h0A1.5 1.5 0 0 1 4 14.5z" />
          <path d="M8 9h8M8 12h5" />
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
