import { Component, OnInit } from '@angular/core';

import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { TeamCardComponent } from '../../../shared/components/team-card/team-card.component';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { TeamService } from '../../../core/services/team.service';
import { Team } from '../../../core/models/team.model';
@Component({
  selector: 'app-teams-list',
  imports: [
    FormsModule,
    NavbarComponent,
    TeamCardComponent,
    LoadingSpinnerComponent,
  ],
  templateUrl: './teams-list.component.html',
  styleUrls: ['./teams-list.component.css'],
})
export class TeamsListComponent {
  teams: Team[] = [];
  filteredTeams: Team[] = [];
  isLoading = false;

  searchTerm = '';
  selectedConference = '';
  selectedDivision = '';

  constructor(private teamService: TeamService) {}

  ngOnInit(): void {
    this.loadTeams();
  }

  loadTeams(): void {
    this.isLoading = true;
    this.teamService.getAllTeams().subscribe({
      next: (response) => {
        this.teams = response.data;
        this.filteredTeams = this.teams;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des équipes:', error);
        this.isLoading = false;
      },
    });
  }

  applyFilters(): void {
    this.filteredTeams = this.teams.filter((team) => {
      const matchesSearch =
        !this.searchTerm ||
        team.name.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        team.city.toLowerCase().includes(this.searchTerm.toLowerCase());

      const matchesConference =
        !this.selectedConference || team.conference === this.selectedConference;

      const matchesDivision =
        !this.selectedDivision || team.division === this.selectedDivision;

      return matchesSearch && matchesConference && matchesDivision;
    });
  }

  resetFilters(): void {
    this.searchTerm = '';
    this.selectedConference = '';
    this.selectedDivision = '';
    this.applyFilters();
  }
}
