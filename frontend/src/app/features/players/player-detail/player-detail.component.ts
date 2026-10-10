import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { PlayerService } from '../../../core/services/player.service';
import { TeamService } from '../../../core/services/team.service';
import { Player, PlayerStatistic, PlayerForm } from '../../../core/models/player.model';
import { Team } from '../../../core/models/team.model';
import { APP_CONSTANTS } from '@core/constants/app-constants';
@Component({
    selector: 'app-player-detail',
    imports: [CommonModule, RouterLink, NavbarComponent, LoadingSpinnerComponent],
    templateUrl: './player-detail.component.html',
    styleUrls: ['./player-detail.component.css']
})
export class PlayerDetailComponent {

  player: Player | null = null;
  team: Team | null = null;
  statistics: PlayerStatistic | null = null;
  playerForm: PlayerForm | null = null;
  isLoading = false;

  private playerId!: number;
  private currentSeason = APP_CONSTANTS.CURRENT_SEASON;

  constructor(
    private route: ActivatedRoute,
    private playerService: PlayerService,
    private teamService: TeamService
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.playerId = +params['id'];
      this.loadPlayerDetails();
    });
  }

  loadPlayerDetails(): void {
    this.isLoading = true;

    this.playerService.getPlayerById(this.playerId).subscribe({
      next: (response) => {
        this.player = response.data;
        if (this.player.teamId) {
          this.loadTeam(this.player.teamId);
        }
        this.loadStatistics();
        this.loadPlayerForm();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erreur:', error);
        this.isLoading = false;
      }
    });
  }

  loadTeam(teamId: number): void {
    this.teamService.getTeamById(teamId).subscribe({
      next: (response) => {
        this.team = response.data;
      },
      error: (error) => console.error('Erreur équipe:', error)
    });
  }

  loadStatistics(): void {
    this.playerService.getPlayerStatistics(this.playerId, this.currentSeason).subscribe({
      next: (response) => {
        this.statistics = response.data;
      },
      error: (error) => console.error('Erreur stats:', error)
    });
  }

  loadPlayerForm(): void {
    this.playerService.getPlayerForm(this.playerId, this.currentSeason).subscribe({
      next: (response) => {
        this.playerForm = response.data;
      },
      error: (error) => console.error('Erreur forme:', error)
    });
  }

  getPositionLabel(position: string): string {
    const positions: Record<string, string> = {
      'PG': 'Meneur (Point Guard)',
      'SG': 'Arrière (Shooting Guard)',
      'SF': 'Ailier (Small Forward)',
      'PF': 'Ailier Fort (Power Forward)',
      'C': 'Pivot (Center)'
    };
    return positions[position] || position;
  }

  getFormColorClass(form: string): string {
    const classes: Record<string, string> = {
      'EXCELLENT': 'bg-green-100 text-green-800',
      'TRÈS BON': 'bg-blue-100 text-blue-800',
      'BON': 'bg-primary-100 text-primary-800',
      'MOYEN': 'bg-yellow-100 text-yellow-800',
      'FAIBLE': 'bg-red-100 text-red-800'
    };
    return classes[form] || 'bg-gray-100 text-gray-800';
  }

  getAge(dateOfBirth: Date): number {
    const today = new Date();
    const birthDate = new Date(dateOfBirth);
    let age = today.getFullYear() - birthDate.getFullYear();
    const monthDiff = today.getMonth() - birthDate.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
      age--;
    }
    return age;
  }
}
