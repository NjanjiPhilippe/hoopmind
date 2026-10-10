import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { PredictionCardComponent } from '../../../shared/components/prediction-card/prediction-card.component';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { PredictionService } from '../../../core/services/prediction.service';
import { MatchPrediction } from '../../../core/models/match.model';

@Component({
    selector: 'app-predictions-list',
    imports: [CommonModule, FormsModule, NavbarComponent, PredictionCardComponent, LoadingSpinnerComponent],
    templateUrl: './predictions-list.component.html',
    styleUrls: ['./predictions-list.component.css']
})
export class PredictionsListComponent implements OnInit {
  predictions: MatchPrediction[] = [];
  isLoading = false;
  minConfidence = 0;
  limit = 20;

  constructor(private predictionService: PredictionService) {}

  ngOnInit(): void {
    this.loadPredictions();
  }

  loadPredictions(): void {
    this.isLoading = true;

    if (this.minConfidence > 0) {
      this.predictionService.getHighConfidencePredictions(this.minConfidence, this.limit).subscribe({
        next: (response) => {
          this.predictions = response.data;
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Erreur:', error);
          this.isLoading = false;
        }
      });
    } else {
      this.predictionService.getUpcomingPredictions(this.limit).subscribe({
        next: (response) => {
          this.predictions = response.data;
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Erreur:', error);
          this.isLoading = false;
        }
      });
    }
  }

  getAverageConfidence(): number {
    if (this.predictions.length === 0) return 0;
    const sum = this.predictions.reduce((acc, p) => acc + p.confidenceScore, 0);
    return sum / this.predictions.length;
  }

  getHighConfidenceCount(): number {
    return this.predictions.filter(p => p.confidenceScore >= 70).length;
  }
}
