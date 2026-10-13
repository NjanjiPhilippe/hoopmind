import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { filter } from 'rxjs';

import { AuthService } from '../../../core/auth/auth.service';

interface NavLink {
  path: string;
  label: string;
  exact?: boolean;
}

/** Les pages de l'espace connecté ; « Admin » s'ajoute pour les administrateurs. */
const LINKS: NavLink[] = [
  { path: '/dashboard', label: 'Accueil', exact: true },
  { path: '/predict', label: 'Prédire' },
  { path: '/standings', label: 'Classement' },
  { path: '/games', label: 'Calendrier' },
];

@Component({
  selector: 'app-navbar',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './navbar.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NavbarComponent {
  protected readonly auth = inject(AuthService);
  protected readonly links = LINKS;
  /** Menu déroulant sous 768 px, où les liens ne tiennent pas dans la barre. */
  protected readonly menuOpen = signal(false);

  constructor() {
    inject(Router).events.pipe(filter(e => e instanceof NavigationEnd), takeUntilDestroyed())
      .subscribe(() => this.menuOpen.set(false));
  }

  logout(): void {
    this.menuOpen.set(false);
    void this.auth.logout();
  }
}
