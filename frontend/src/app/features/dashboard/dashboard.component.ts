import { Component, OnInit } from '@angular/core';

import { RouterLink } from '@angular/router';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { StatCardComponent } from '../../shared/components/stat-card/stat-card.component';
import { PredictionCardComponent } from '../../shared/components/prediction-card/prediction-card.component';
import { TeamCardComponent } from '../../shared/components/team-card/team-card.component';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';
import { DashboardService } from '../../core/services/dashboard.service';
import { Dashboard } from '../../core/models/dashboard.model';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-dashboard',
  imports: [
    RouterLink,
    NavbarComponent,
    StatCardComponent,
    PredictionCardComponent,
    TeamCardComponent,
    LoadingSpinnerComponent,
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
})
export class DashboardComponent {
  dashboard: Dashboard | null = null;
  isLoading = false;
  currentUser = '';

  constructor(
    private dashboardService: DashboardService,
    private authService: AuthService,
  ) {}

  ngOnInit(): void {
    const user = this.authService.user();
    this.currentUser = user?.username || 'Utilisateur';
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.isLoading = true;
    this.dashboardService.getDashboard().subscribe({
      next: (response) => {
        this.dashboard = response.data;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement du dashboard:', error);
        this.isLoading = false;
      },
    });
  }
}
