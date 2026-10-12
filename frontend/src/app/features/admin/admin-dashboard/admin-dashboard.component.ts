import { DatePipe, DecimalPipe, PercentPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, resource } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Api } from '../../../core/api/generated/api';
import { listUsers } from '../../../core/api/generated/fn/administration-des-utilisateurs/list-users';
import { getNbaDatabaseStatus } from '../../../core/api/generated/fn/import-des-donnees/get-nba-database-status';
import { listAuditEvents } from '../../../core/api/generated/fn/journal-d-audit/list-audit-events';
import { getModelMetrics } from '../../../core/api/generated/fn/spark-analytics/get-model-metrics';
import { TrainingReport } from '../../../core/api/generated/models/training-report';
import { currentValue } from '../../../core/util/resource';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { AUDIT_ACTION_LABELS } from '../audit/audit-labels';
import { gainOverBaseline, phaseLabel } from '../data/data-labels';

/**
 * Tableau de bord de l'administration : un chiffre réel par rubrique (comptes, données, modèle, journal) et un lien
 * vers la page qui agit dessus. Aucun bouton sans action, aucun chiffre écrit en dur.
 */
@Component({
  selector: 'app-admin-dashboard',
  imports: [DatePipe, DecimalPipe, PercentPipe, RouterLink, NavbarComponent],
  templateUrl: './admin-dashboard.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminDashboardComponent {
  protected readonly currentValue = currentValue;
  private readonly api = inject(Api);
  protected readonly actionLabels = AUDIT_ACTION_LABELS;
  protected readonly phaseLabel = phaseLabel;

  protected readonly users = resource({ loader: () => this.api.invoke(listUsers, { page: 0, size: 1 }) });
  protected readonly data = resource({ loader: () => this.api.invoke(getNbaDatabaseStatus) });
  protected readonly model = resource({ loader: () => this.latestModel() });
  protected readonly audit = resource({ loader: () => this.api.invoke(listAuditEvents, { page: 0, size: 5 }) });

  protected readonly userCount = computed(() => currentValue(this.users)?.page?.totalElements ?? null);
  protected readonly lastImport = computed(() => currentValue(this.data)?.job ?? null);
  protected readonly gain = computed(() => gainOverBaseline(currentValue(this.model)));

  private async latestModel(): Promise<TrainingReport | null> {
    try {
      return await this.api.invoke(getModelMetrics);
    } catch (e) {
      if (e instanceof HttpErrorResponse && (e.status === 404 || e.status === 503)) {
        return null;
      }
      throw e;
    }
  }
}
