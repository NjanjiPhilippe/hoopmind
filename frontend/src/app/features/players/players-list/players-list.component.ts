import { Component, OnInit } from '@angular/core';

import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { TeamService } from '../../../core/services/team.service';
import { PlayerService } from '../../../core/services/player.service';
import { Team } from '../../../core/models/team.model';
import { Player } from '../../../core/models/player.model';

@Component({
  selector: 'app-players-list',
  imports: [FormsModule, RouterLink, NavbarComponent, LoadingSpinnerComponent],
  templateUrl: './players-list.component.html',
  styleUrls: ['./players-list.component.css'],
})
export class PlayersListComponent {
  teams: Team[] = [];
  players: Player[] = [];
  filteredPlayers: Player[] = [];
  isLoading = false;

  searchTerm = '';
  selectedTeamId: number | string = '';
  selectedPosition = '';

  constructor(
    private teamService: TeamService,
    private playerService: PlayerService,
  ) {}

  ngOnInit(): void {
    this.loadTeams();
  }

  loadTeams(): void {
    this.teamService.getAllTeams().subscribe({
      next: (response) => {
        this.teams = response.data;
      },
      error: (error) => console.error('Erreur:', error),
    });
  }

  onSearch(): void {
    if (this.searchTerm.length >= 2) {
      this.isLoading = true;
      this.playerService.searchPlayers(this.searchTerm).subscribe({
        next: (response) => {
          this.players = response.data;
          this.applyFilters();
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Erreur:', error);
          this.isLoading = false;
        },
      });
    } else if (this.searchTerm.length === 0 && this.selectedTeamId) {
      this.loadPlayersByTeam(+this.selectedTeamId);
    } else {
      this.players = [];
      this.filteredPlayers = [];
    }
  }

  onTeamChange(): void {
    if (this.selectedTeamId) {
      this.loadPlayersByTeam(+this.selectedTeamId);
    } else {
      this.players = [];
      this.filteredPlayers = [];
    }
  }

  loadPlayersByTeam(teamId: number): void {
    this.isLoading = true;
    this.playerService.getPlayersByTeam(teamId).subscribe({
      next: (response) => {
        this.players = response.data;
        this.applyFilters();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erreur:', error);
        this.isLoading = false;
      },
    });
  }

  applyFilters(): void {
    this.filteredPlayers = this.players.filter((player) => {
      const matchesPosition =
        !this.selectedPosition || player.position === this.selectedPosition;
      return matchesPosition;
    });
  }

  resetFilters(): void {
    this.searchTerm = '';
    this.selectedTeamId = '';
    this.selectedPosition = '';
    this.players = [];
    this.filteredPlayers = [];
  }
}
