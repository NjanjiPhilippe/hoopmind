import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, linkedSignal, numberAttribute, resource } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { Api } from '../../../core/api/generated/api';
import { listAuditEvents } from '../../../core/api/generated/fn/journal-d-audit/list-audit-events';
import { AuditEvent } from '../../../core/api/generated/models/audit-event';
import { currentValue } from '../../../core/util/resource';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { AUDIT_ACTION_LABELS, AUDIT_ACTIONS, AuditAction, describeDetails } from './audit-labels';

const PAGE_SIZE = 50;

interface Filters {
  user: string;
  action: AuditAction | '';
  outcome: AuditEvent['outcome'] | '';
  from: string;
  to: string;
}

const DATE = /^\d{4}-\d{2}-\d{2}$/;

/**
 * Filtres lus dans l'URL : une valeur inconnue (lien modifié à la main) est ignorée au lieu d'être envoyée à l'API.
 */
export function filtersFromUrl(params: { user?: string; action?: string; outcome?: string; from?: string; to?: string }): Filters {
  return {
    user: params.user?.trim() ?? '',
    action: AUDIT_ACTIONS.includes(params.action as AuditAction) ? (params.action as AuditAction) : '',
    outcome: params.outcome === 'SUCCESS' || params.outcome === 'FAILURE' ? params.outcome : '',
    from: params.from && DATE.test(params.from) ? params.from : '',
    to: params.to && DATE.test(params.to) ? params.to : '',
  };
}

/** Début de la journée locale, au format ISO attendu par l'API. */
function startOfDay(date: string, plusDays = 0): string | undefined {
  if (!date) return undefined;
  const day = new Date(`${date}T00:00:00`);
  day.setDate(day.getDate() + plusDays);
  return day.toISOString();
}

@Component({
  selector: 'app-admin-audit',
  imports: [DatePipe, RouterLink, NavbarComponent],
  templateUrl: './admin-audit.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminAuditComponent {
  private readonly api = inject(Api);
  private readonly router = inject(Router);

  /** Filtre sur un utilisateur précis, depuis le lien « Historique » de la page des utilisateurs. */
  readonly userId = input(undefined, { transform: (value: unknown) => (value == null || value === '' ? undefined : numberAttribute(value)) });
  readonly userLabel = input<string>();

  // Les filtres et la page sont dans l'URL : un filtre se partage, survit au rechargement et suit le bouton Retour.
  readonly user = input<string>();
  readonly action = input<string>();
  readonly outcome = input<string>();
  readonly from = input<string>();
  readonly to = input<string>();
  readonly page = input(0, { transform: (value: unknown) => Math.max(0, numberAttribute(value, 0)) });

  protected readonly actions = AUDIT_ACTIONS;
  protected readonly actionLabels = AUDIT_ACTION_LABELS;
  protected readonly describe = describeDetails;

  protected readonly filters = computed(() => filtersFromUrl({
    user: this.user(), action: this.action(), outcome: this.outcome(), from: this.from(), to: this.to(),
  }));
  protected readonly filtered = computed(() => Object.values(this.filters()).some(Boolean));
  /** Texte saisi dans la recherche, appliqué à l'URL après une courte pause de frappe. */
  protected readonly userText = linkedSignal(() => this.filters().user);

  protected readonly events = resource({
    params: () => {
      const f = this.filters();
      return {
        userId: this.userId(),
        user: f.user || undefined,
        action: f.action || undefined,
        outcome: f.outcome || undefined,
        from: startOfDay(f.from),
        to: startOfDay(f.to, 1),
        page: this.page(),
        size: PAGE_SIZE,
      };
    },
    loader: ({ params }) => this.api.invoke(listAuditEvents, params),
  });
  protected readonly rows = computed(() => currentValue(this.events)?.content ?? []);
  protected readonly pageInfo = computed(() => currentValue(this.events)?.page);

  private userTimer?: ReturnType<typeof setTimeout>;

  onUser(value: string): void {
    this.userText.set(value);
    clearTimeout(this.userTimer);
    this.userTimer = setTimeout(() => this.update({ user: value.trim() }), 300);
  }

  /** Change un filtre et revient à la première page. */
  update(change: Partial<Filters>): void {
    const queryParams: Record<string, string | null> = { page: null };
    for (const [key, value] of Object.entries(change)) {
      queryParams[key] = value || null;
    }
    void this.router.navigate([], { queryParams, queryParamsHandling: 'merge' });
  }

  goToPage(page: number): void {
    void this.router.navigate([], { queryParams: { page: page || null }, queryParamsHandling: 'merge' });
  }

  reset(): void {
    clearTimeout(this.userTimer);
    void this.router.navigate([], {
      queryParams: { user: null, action: null, outcome: null, from: null, to: null, page: null },
      queryParamsHandling: 'merge',
    });
  }
}
