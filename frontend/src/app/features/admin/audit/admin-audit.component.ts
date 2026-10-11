import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, linkedSignal, numberAttribute, resource, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

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

const NO_FILTERS: Filters = { user: '', action: '', outcome: '', from: '', to: '' };

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

  /** Filtre sur un utilisateur précis, depuis le lien « Historique » de la page des utilisateurs. */
  readonly userId = input(undefined, { transform: (value: unknown) => (value == null || value === '' ? undefined : numberAttribute(value)) });
  readonly userLabel = input<string>();

  protected readonly actions = AUDIT_ACTIONS;
  protected readonly actionLabels = AUDIT_ACTION_LABELS;
  protected readonly describe = describeDetails;

  protected readonly filters = signal<Filters>(NO_FILTERS);
  /** Revient à la première page à chaque changement de filtre. */
  protected readonly page = linkedSignal({ source: () => [this.filters(), this.userId()], computation: () => 0 });

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
    clearTimeout(this.userTimer);
    this.userTimer = setTimeout(() => this.update({ user: value.trim() }), 300);
  }

  update(change: Partial<Filters>): void {
    this.filters.update(current => ({ ...current, ...change }));
  }

  reset(): void {
    clearTimeout(this.userTimer);
    this.filters.set(NO_FILTERS);
  }
}
