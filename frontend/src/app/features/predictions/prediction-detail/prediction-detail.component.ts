import { Component, OnInit } from '@angular/core';

import { ActivatedRoute, RouterLink } from '@angular/router';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { PredictionService } from '../../../core/services/prediction.service';
import { MatchPrediction } from '../../../core/models/match.model';

@Component({
  selector: 'app-prediction-detail',
  imports: [RouterLink, NavbarComponent, LoadingSpinnerComponent],
  templateUrl: './prediction-detail.component.html',
  styleUrls: ['./prediction-detail.component.css'],
})
export class PredictionDetailComponent {
  prediction: MatchPrediction | null = null;
  isLoading = false;

  constructor(
    private route: ActivatedRoute,
    private predictionService: PredictionService,
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe((params) => {
      const id = +params['id'];
      this.loadPrediction(id);
    });
  }

  loadPrediction(id: number): void {
    this.isLoading = true;
    this.predictionService.getPredictionById(id).subscribe({
      next: (response) => {
        this.prediction = response.data;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erreur:', error);
        this.isLoading = false;
      },
    });
  }
}
