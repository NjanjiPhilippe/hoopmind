import { Component, Input } from '@angular/core';

import { RouterLink } from '@angular/router';
import { Team } from '../../../core/models/team.model';
import { APP_CONSTANTS } from '../../../core/constants/app-constants';
@Component({
  selector: 'app-team-card',
  imports: [RouterLink],
  templateUrl: './team-card.component.html',
  styleUrls: ['./team-card.component.css'],
})
export class TeamCardComponent {
  @Input() team!: Team;

  get teamLogo(): string {
    return this.team.logoUrl || APP_CONSTANTS.DEFAULT_TEAM_LOGO;
  }

  onImageError(event: any): void {
    event.target.src = APP_CONSTANTS.DEFAULT_TEAM_LOGO;
  }
}
