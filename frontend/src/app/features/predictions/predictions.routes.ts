import { Routes } from '@angular/router';

export const PREDICTIONS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./predictions-list/predictions-list.component').then(m => m.PredictionsListComponent)
  },
  {
    path: ':id',
    loadComponent: () => import('./prediction-detail/prediction-detail.component').then(m => m.PredictionDetailComponent)
  }
];