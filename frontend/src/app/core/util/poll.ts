import { DestroyRef, effect, inject } from '@angular/core';

/**
 * Relance {@code reload} toutes les {@code intervalMs} tant que {@code running} est vrai. {@code running} doit lire la
 * valeur rechargée (par exemple {@code resource.value()}) : chaque relecture produit une nouvelle valeur, ce qui relance
 * l'effet tant que le travail suivi n'est pas fini. À appeler dans un contexte d'injection (constructeur).
 */
export function pollWhile(running: () => boolean, reload: () => void, intervalMs: number): void {
  let timer: ReturnType<typeof setTimeout> | undefined;
  effect(() => {
    clearTimeout(timer);
    if (running()) {
      timer = setTimeout(reload, intervalMs);
    }
  });
  inject(DestroyRef).onDestroy(() => clearTimeout(timer));
}
