import { ChangeDetectionStrategy, Component, computed, input, linkedSignal } from '@angular/core';

import { TeamResponse } from '../../../core/api/generated/models/team-response';
import { TeamSummaryResponse } from '../../../core/api/generated/models/team-summary-response';
import { teamColor, teamLogoUrl, textOn } from '../../../core/nba/team-branding';

/**
 * Logo d'une équipe. Si l'image manque ou ne se charge pas (hors ligne, franchise inconnue), une pastille aux
 * couleurs de l'équipe porte son abréviation. Décoratif : le nom de l'équipe est toujours écrit à côté.
 */
@Component({
  selector: 'app-team-logo',
  template: `
    @if (url(); as src) {
      <img [src]="src" alt="" aria-hidden="true" loading="lazy" decoding="async" class="shrink-0 object-contain"
           [style.width.px]="size()" [style.height.px]="size()" (error)="failed.set(true)" />
    } @else {
      <span aria-hidden="true" class="inline-flex shrink-0 items-center justify-center rounded-full font-bold leading-none"
            [style.width.px]="size()" [style.height.px]="size()" [style.font-size.px]="size() * 0.32"
            [style.background-color]="color()" [style.color]="text()">{{ team()?.abbreviation }}</span>
    }
  `,
  host: { class: 'inline-flex' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TeamLogoComponent {
  readonly team = input<TeamResponse | TeamSummaryResponse | null | undefined>();
  readonly size = input(24);

  protected readonly failed = linkedSignal({ source: () => this.team(), computation: () => false });
  protected readonly url = computed(() => (this.failed() ? null : teamLogoUrl(this.team())));
  protected readonly color = computed(() => teamColor(this.team()));
  protected readonly text = computed(() => textOn(this.color()));
}
