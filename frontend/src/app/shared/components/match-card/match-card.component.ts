import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Match, MatchStatus } from '../../../core/models/match.model';
@Component({
    selector: 'app-match-card',
    imports: [CommonModule, RouterLink],
    templateUrl: './match-card.component.html',
    styleUrls: ['./match-card.component.css']
})
export class MatchCardComponent {
 @Input() match!: Match;

  get statusLabel(): string {
    const labels: Record<MatchStatus, string> = {
      [MatchStatus.SCHEDULED]: 'À venir',
      [MatchStatus.IN_PROGRESS]: 'En cours',
      [MatchStatus.COMPLETED]: 'Terminé',
      [MatchStatus.CANCELLED]: 'Annulé'
    };
    return labels[this.match.status];
  }

  get statusBadgeClass(): string {
    const classes: Record<MatchStatus, string> = {
      [MatchStatus.SCHEDULED]: 'badge-info',
      [MatchStatus.IN_PROGRESS]: 'badge-warning',
      [MatchStatus.COMPLETED]: 'badge-success',
      [MatchStatus.CANCELLED]: 'badge-danger'
    };
    return classes[this.match.status];
  }
}
