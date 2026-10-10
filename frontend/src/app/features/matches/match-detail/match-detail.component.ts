import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { MatchService } from '../../../core/services/match.service';
import { PredictionService } from '../../../core/services/prediction.service';
import { Match, MatchPrediction, MatchStatus } from '../../../core/models/match.model';

@Component({
    selector: 'app-match-detail',
    imports: [CommonModule, RouterLink, NavbarComponent, LoadingSpinnerComponent],
    templateUrl: './match-detail.component.html',
    styleUrls: ['./match-detail.component.css']
})
export class MatchDetailComponent {
match: Match | null = null;
  prediction: MatchPrediction | null = null;
  predictionFactors: any = null;
  isLoading = false;
  isGenerating = false;

  private matchId!: number;

  constructor(
    private route: ActivatedRoute,
    private matchService: MatchService,
    private predictionService: PredictionService
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.matchId = +params['id'];
      this.loadMatchDetails();
    });
  }

  loadMatchDetails(): void {
    this.isLoading = true;

    this.matchService.getMatchById(this.matchId).subscribe({
      next: (response) => {
        this.match = response.data;
        this.prediction = this.match.prediction || null;
        
        if (this.prediction?.factors) {
          try {
            this.predictionFactors = JSON.parse(this.prediction.factors);
          } catch (e) {
            console.error('Erreur parsing factors:', e);
          }
        }
        
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erreur:', error);
        this.isLoading = false;
      }
    });
  }

  generatePrediction(): void {
    this.isGenerating = true;

    this.predictionService.predictMatch(this.matchId).subscribe({
      next: (response) => {
        this.prediction = response.data;
        if (this.prediction?.factors) {
          try {
            this.predictionFactors = JSON.parse(this.prediction.factors);
          } catch (e) {
            console.error('Erreur parsing factors:', e);
          }
        }
        this.isGenerating = false;
      },
      error: (error) => {
        console.error('Erreur génération:', error);
        this.isGenerating = false;
      }
    });
  }

  getStatusLabel(): string {
    if (!this.match) return '';
    const labels: Record<MatchStatus, string> = {
      [MatchStatus.SCHEDULED]: 'À venir',
      [MatchStatus.IN_PROGRESS]: 'En cours',
      [MatchStatus.COMPLETED]: 'Terminé',
      [MatchStatus.CANCELLED]: 'Annulé'
    };
    return labels[this.match.status];
  }

  getStatusBadgeClass(): string {
    if (!this.match) return '';
    const classes: Record<MatchStatus, string> = {
      [MatchStatus.SCHEDULED]: 'badge-info',
      [MatchStatus.IN_PROGRESS]: 'badge-warning',
      [MatchStatus.COMPLETED]: 'badge-success',
      [MatchStatus.CANCELLED]: 'badge-danger'
    };
    return classes[this.match.status];
  }

  getConfidenceBadgeClass(): string {
    if (!this.prediction) return '';
    const confidence = this.prediction.confidenceScore;
    if (confidence >= 80) return 'badge-success';
    if (confidence >= 60) return 'badge-info';
    if (confidence >= 40) return 'badge-warning';
    return 'badge-danger';
  }

  getPredictedWinner(): string {
    if (!this.prediction) return '';
    return this.prediction.homeWinProbability > this.prediction.awayWinProbability
      ? this.prediction.homeTeamName
      : this.prediction.awayTeamName;
  }

  formatFactorKey(key: string): string {
    return key.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase());
  }
}
