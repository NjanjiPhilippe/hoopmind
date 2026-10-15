import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { FooterComponent } from '../../shared/components/footer/footer.component';
import { IconComponent } from '../../shared/components/icon/icon.component';

/** Photo du bandeau : « The Last Shot » de Michael Jordan (finale 1998), chargée par lien (aucune copie dans le dépôt). */
const HERO_PHOTO = {
  src: 'https://www.michaeljordansworld.com/pictures/images/michael_jordan_bulls_last_shot.jpg',
  page: 'https://www.michaeljordansworld.com/pictures/michael_jordan_bulls_last_shot.htm',
};

/** Page d'accueil publique : ce que fait HoopMind, sans appel à l'API (un visiteur n'a pas accès aux données). */
@Component({
  selector: 'app-home',
  imports: [RouterLink, NavbarComponent, FooterComponent, IconComponent],
  templateUrl: './home.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomeComponent {
  protected readonly auth = inject(AuthService);
  protected readonly photo = HERO_PHOTO;
  /** Hors ligne ou lien cassé : le bandeau reste noir, sans crédit. */
  protected readonly photoMissing = signal(false);
}
