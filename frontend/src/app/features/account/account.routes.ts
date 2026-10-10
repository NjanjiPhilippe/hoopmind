import { Routes } from '@angular/router';

export const ACCOUNT_ROUTES: Routes = [
  {
    path: '',
    title: 'Mon compte · HoopMind',
    loadComponent: () => import('./profile/profile.component').then(m => m.ProfileComponent),
  },
];
