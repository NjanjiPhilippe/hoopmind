import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * Illustration du panneau gauche de la connexion : un joueur en fade away, façon pictogramme, à la couleur du
 * logo. Sert de repli quand aucune photo locale (`public/login-hero.jpg`, ignorée par git) n'est fournie.
 */
@Component({
  selector: 'app-login-illustration',
  template: `
    <svg viewBox="0 0 400 520" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round"
         aria-hidden="true" focusable="false" class="h-full w-full">
      <!-- Traits de mouvement : le joueur recule en tirant -->
      <g stroke-opacity="0.25" stroke-width="3">
        <path d="M70 150c-14 40-14 90 2 132" />
        <path d="M44 178c-8 30-8 62 2 92" />
        <path d="M96 330c-10 26-10 52 0 76" />
      </g>
      <ellipse cx="205" cy="492" rx="90" ry="7" stroke="none" fill="currentColor" fill-opacity="0.18" />
      <!-- Corps penché en arrière, genou avant levé, bras tendus vers le panier -->
      <path d="M164 182 206 282" stroke-width="58" />
      <path d="M166 176 204 114 230 66" stroke-width="22" />
      <path d="M180 184 232 132 248 84" stroke-width="20" />
      <path d="M204 286 268 300 258 386" stroke-width="32" />
      <path d="M258 386 282 392" stroke-width="20" />
      <path d="M196 292 176 384 140 446" stroke-width="32" />
      <path d="M140 446 120 452" stroke-width="20" />
      <circle cx="146" cy="124" r="27" stroke="none" fill="currentColor" />
      <!-- Le ballon, dessiné comme le logo -->
      <g stroke-width="5">
        <circle cx="262" cy="38" r="24" />
        <path d="M262 14v48M238 38h48M246 20c9 10 9 26 0 36M278 20c-9 10-9 26 0 36" />
      </g>
    </svg>
  `,
  host: { class: 'block text-primary-500' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginIllustrationComponent {}
