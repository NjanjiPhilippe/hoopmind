import { Routes } from '@angular/router';

import { adminGuard, authGuard, guestGuard } from './core/auth/auth.guards';

export const routes: Routes = [
  {
    path: '',
    title: 'HoopMind',
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
    title: 'Accueil · HoopMind',
    canActivate: [authGuard],
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
  },
  {
    path: 'predict',
    title: 'Prédire un match · HoopMind',
    canActivate: [authGuard],
    loadComponent: () => import('./features/predict/predict.component').then(m => m.PredictComponent),
  },
  {
    path: 'predictions',
    title: 'Mes prédictions · HoopMind',
    canActivate: [authGuard],
    loadComponent: () => import('./features/predictions/my-predictions.component').then(m => m.MyPredictionsComponent),
  },
  {
    path: 'standings',
    title: 'Classement · HoopMind',
    canActivate: [authGuard],
    loadComponent: () => import('./features/standings/standings.component').then(m => m.StandingsComponent),
  },
  {
    path: 'games',
    title: 'Calendrier · HoopMind',
    canActivate: [authGuard],
    loadComponent: () => import('./features/games/games.component').then(m => m.GamesComponent),
  },
  {
    path: 'forbidden',
    title: 'Accès refusé · HoopMind',
    data: { kind: 'forbidden' },
    loadComponent: () => import('./features/errors/error-page.component').then(m => m.ErrorPageComponent),
  },
  {
    path: '**',
    title: 'Page introuvable · HoopMind',
    data: { kind: 'not-found' },
    loadComponent: () => import('./features/errors/error-page.component').then(m => m.ErrorPageComponent),
  },
];
