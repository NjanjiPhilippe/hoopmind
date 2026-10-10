import { Routes } from '@angular/router';

export const MATCHES_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./matches-list/matches-list.component').then(m => m.MatchesListComponent)
  },
  {
    path: ':id',
    loadComponent: () => import('./match-detail/match-detail.component').then(m => m.MatchDetailComponent)
  }
];