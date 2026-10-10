import { Routes } from '@angular/router';

import { adminGuard, authGuard, guestGuard } from './core/auth/auth.guards';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/home/home.component').then(m => m.HomeComponent),
  },
  {
    path: 'auth',
    canActivate: [guestGuard],
    loadChildren: () => import('./features/auth/auth.routes').then(m => m.AUTH_ROUTES),
  },
  {
    path: 'account',
    canActivate: [authGuard],
    loadChildren: () => import('./features/account/account.routes').then(m => m.ACCOUNT_ROUTES),
  },
  {
    path: 'admin',
    canActivate: [adminGuard],
    loadChildren: () => import('./features/admin/admin.routes').then(m => m.ADMIN_ROUTES),
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
  },
  {
    path: 'teams',
    canActivate: [authGuard],
    loadChildren: () => import('./features/teams/teams.routes').then(m => m.TEAMS_ROUTES),
  },
  {
    path: 'players',
    canActivate: [authGuard],
    loadChildren: () => import('./features/players/players.routes').then(m => m.PLAYERS_ROUTES),
  },
  {
    path: 'matches',
    canActivate: [authGuard],
    loadChildren: () => import('./features/matches/matches.routes').then(m => m.MATCHES_ROUTES),
  },
  {
    path: 'predictions',
    canActivate: [authGuard],
    loadChildren: () => import('./features/predictions/predictions.routes').then(m => m.PREDICTIONS_ROUTES),
  },
  {
    path: 'statistics',
    canActivate: [authGuard],
    loadComponent: () => import('./features/statistics/statistics.component').then(m => m.StatisticsComponent),
  },
  {
    path: '**',
    redirectTo: '',
  },
];
