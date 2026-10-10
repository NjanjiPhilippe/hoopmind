import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { MatchCardComponent } from '../../../shared/components/match-card/match-card.component';
import { TeamService } from '../../../core/services/team.service';
import { PlayerService } from '../../../core/services/player.service';
import { MatchService } from '../../../core/services/match.service';
import { Team, TeamStatistic, TeamForm } from '../../../core/models/team.model';
import { Player } from '../../../core/models/player.model';
import { Match } from '../../../core/models/match.model';
import { APP_CONSTANTS } from '@core/constants/app-constants';

@Component({
    selector: 'app-team-detail',
    imports: [CommonModule, RouterLink, NavbarComponent, LoadingSpinnerComponent, MatchCardComponent],
    templateUrl: './team-detail.component.html',
    styleUrls: ['./team-detail.component.css']
})
export class TeamDetailComponent implements OnInit {
  team: Team | null = null;
  statistics: TeamStatistic | null = null;
  teamForm: TeamForm | null = null;
  players: Player[] = [];
  recentMatches: Match[] = [];
  isLoading = false;

  private teamId!: number;
  private currentSeason = APP_CONSTANTS.CURRENT_SEASON;

  constructor(
    private route: ActivatedRoute,
    private teamService: TeamService,
    private playerService: PlayerService,
    private matchService: MatchService
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.teamId = +params['id'];
      this.loadTeamDetails();
    });
  }

  loadTeamDetails(): void {
    this.isLoading = true;

    this.teamService.getTeamById(this.teamId).subscribe({
      next: (response) => {
        this.team = response.data;
        this.loadStatistics();
        this.loadTeamForm();
        this.loadPlayers();
        this.loadRecentMatches();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erreur:', error);
        this.isLoading = false;
      }
    });
  }

  loadStatistics(): void {
    this.teamService.getTeamStatistics(this.teamId, this.currentSeason).subscribe({
      next: (response) => {
        this.statistics = response.data;
      },
      error: (error) => console.error('Erreur stats:', error)
    });
  }

  loadTeamForm(): void {
    this.teamService.getTeamForm(this.teamId, 10).subscribe({
      next: (response) => {
        this.teamForm = response.data;
      },
      error: (error) => console.error('Erreur forme:', error)
    });
  }

  loadPlayers(): void {
    this.playerService.getPlayersByTeam(this.teamId).subscribe({
      next: (response) => {
        this.players = response.data;
      },
      error: (error) => console.error('Erreur joueurs:', error)
    });
  }

  loadRecentMatches(): void {
    this.matchService.getTeamMatches(this.teamId, this.currentSeason).subscribe({
      next: (response) => {
        this.recentMatches = response.data.slice(0, 6);
      },
      error: (error) => console.error('Erreur matchs:', error)
    });
  }

  get teamLogo(): string {
    return this.team?.logoUrl || APP_CONSTANTS.DEFAULT_TEAM_LOGO;
  }

  onImageError(event: any): void {
    event.target.src = APP_CONSTANTS.DEFAULT_TEAM_LOGO;
  }
}
