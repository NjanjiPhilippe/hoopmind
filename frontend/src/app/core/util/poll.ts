import { DestroyRef, effect, inject } from '@angular/core';

/**
 * Relance {@code reload} toutes les {@code intervalMs} tant que {@code running} est vrai. {@code running} doit lire la
 * valeur rechargée (par exemple {@code resource.value()}) : chaque relecture produit une nouvelle valeur, ce qui relance
 * l'effet tant que le travail suivi n'est pas fini. À appeler dans un contexte d'injection (constructeur).
 *
 * {@code failed} dit si la dernière relecture a échoué : la valeur est alors inconnue, et le suivi continue si le
 * travail tournait à la relecture précédente, au lieu de le croire fini après une seule erreur réseau.
 */
export function pollWhile(running: () => boolean, reload: () => void, intervalMs: number,
                          failed: () => boolean = () => false): void {
  let timer: ReturnType<typeof setTimeout> | undefined;
  let wasRunning = false;
  effect(() => {
    clearTimeout(timer);
    if (!failed()) {
      wasRunning = running();
    }
    if (wasRunning) {
      timer = setTimeout(reload, intervalMs);
    }
  });
  inject(DestroyRef).onDestroy(() => clearTimeout(timer));
}
