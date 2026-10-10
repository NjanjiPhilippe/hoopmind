import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatchPrediction } from '../../../core/models/match.model';
@Component({
    selector: 'app-prediction-card',
    imports: [CommonModule, RouterLink],
    templateUrl: './prediction-card.component.html',
    styleUrls: ['./prediction-card.component.css']
})
export class PredictionCardComponent {

   @Input() prediction!: MatchPrediction;

  get confidenceBadgeClass(): string {
    const confidence = this.prediction.confidenceScore;
    if (confidence >= 80) return 'badge-success';
    if (confidence >= 60) return 'badge-info';
    if (confidence >= 40) return 'badge-warning';
    return 'badge-danger';
  }

  get predictedWinner(): string {
    return this.prediction.homeWinProbability > this.prediction.awayWinProbability
      ? this.prediction.homeTeamName
      : this.prediction.awayTeamName;
  }
}
