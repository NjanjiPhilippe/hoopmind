import { ChangeDetectionStrategy, Component } from '@angular/core';

/** Pied de page : nom de l'application et crédit des données (licence CC BY-SA 4.0 de la base NBA). */
@Component({
  selector: 'app-footer',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <footer class="bg-gray-900 py-8 text-center text-sm text-gray-400">
      <p>© {{ year }} HoopMind · Prédictions de matchs NBA</p>
      <p class="mt-1">
        Données : « NBA Database » de Wyatt Walsh (Kaggle, CC BY-SA 4.0) et « NBA Stats (1947-present) » de Sumitro Datta.
        HoopMind n'est pas affilié à la NBA.
      </p>
    </footer>
  `,
})
export class FooterComponent {
  protected readonly year = new Date().getFullYear();
}
