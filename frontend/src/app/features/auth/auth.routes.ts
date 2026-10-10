import { Routes } from '@angular/router';

export const AUTH_ROUTES: Routes = [
  {
    path: 'login',
    title: 'Connexion · HoopMind',
    loadComponent: () => import('./login/login.component').then(m => m.LoginComponent),
  },
  {
    path: 'register',
    title: 'Inscription · HoopMind',
    loadComponent: () => import('./register/register.component').then(m => m.RegisterComponent),
  },
];
