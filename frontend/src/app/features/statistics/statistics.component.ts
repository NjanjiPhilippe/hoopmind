import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';
import { StatisticsService } from '../../core/services/statistics.service';
import { TeamStatistic } from '../../core/models/team.model';
import { PlayerStatistic } from '../../core/models/player.model';
import { APP_CONSTANTS } from '@core/constants/app-constants';

@Component({
    selector: 'app-statistics',
    imports: [CommonModule, FormsModule, RouterLink, NavbarComponent, LoadingSpinnerComponent],
    templateUrl: './statistics.component.html',
    styleUrls: ['./statistics.component.css']
})
export class StatisticsComponent {
topTeams: TeamStatistic[] = [];
  topScorers: PlayerStatistic[] = [];
  topAssistants: PlayerStatistic[] = [];
  isLoading = false;
  selectedSeason = APP_CONSTANTS.CURRENT_SEASON;

  constructor(private statisticsService: StatisticsService) {}

  ngOnInit(): void {
    this.loadStatistics();
  }

  loadStatistics(): void {
    this.isLoading = true;

    this.statisticsService.getTopTeams(this.selectedSeason, 10).subscribe({
      next: (response) => {
        this.topTeams = response.data;
      },
      error: (error) => console.error('Erreur:', error)
    });

    this.statisticsService.getTopScorers(this.selectedSeason, 5).subscribe({
      next: (response) => {
        this.topScorers = response.data;
      },
      error: (error) => console.error('Erreur:', error)
    });

    this.statisticsService.getTopAssistants(this.selectedSeason, 5).subscribe({
      next: (response) => {
        this.topAssistants = response.data;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erreur:', error);
        this.isLoading = false;
      }
    });
  }
}
