import { Routes } from '@angular/router';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    title: 'Administration · HoopMind',
    loadComponent: () => import('./admin-dashboard/admin-dashboard.component').then(m => m.AdminDashboardComponent),
  },
  {
    path: 'users',
    title: 'Utilisateurs · HoopMind',
    loadComponent: () => import('./users/admin-users.component').then(m => m.AdminUsersComponent),
  },
  {
    path: 'data',
    title: 'Données · HoopMind',
    loadComponent: () => import('./data/admin-data.component').then(m => m.AdminDataComponent),
  },
  {
    path: 'results',
    title: 'Saisie des résultats · HoopMind',
    loadComponent: () => import('./results/admin-results.component').then(m => m.AdminResultsComponent),
  },
  {
    path: 'players',
    title: 'Joueurs · HoopMind',
    loadComponent: () => import('./players/admin-players.component').then(m => m.AdminPlayersComponent),
  },
  {
    path: 'audit',
    title: "Journal d'audit · HoopMind",
    loadComponent: () => import('./audit/admin-audit.component').then(m => m.AdminAuditComponent),
  },
];
