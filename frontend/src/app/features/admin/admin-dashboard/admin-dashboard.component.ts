import { Component, OnInit } from '@angular/core';

import { RouterLink } from '@angular/router';

import { DashboardService } from '@core/services/dashboard.service';
import { NavbarComponent } from '@shared/components/navbar/navbar.component';
import { StatCardComponent } from '@shared/components/stat-card/stat-card.component';
@Component({
  selector: 'app-admin-dashboard',
  imports: [RouterLink, NavbarComponent, StatCardComponent],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.css'],
})
export class AdminDashboardComponent implements OnInit {
  constructor(private dashboardService: DashboardService) {}

  ngOnInit(): void {
    // Load admin stats
  }
}
