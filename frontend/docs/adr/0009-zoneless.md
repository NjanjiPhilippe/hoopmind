# 0009 · L'application tourne sans zone.js

## Problème

zone.js intercepte chaque tâche asynchrone pour déclencher la détection des changements. Depuis Angular 20, le
mode sans zone est stable, et c'est le mode par défaut des nouveaux projets Angular 21. Après la refonte, toutes
les pages lisent leur état dans des signals et sont en `OnPush` : zone.js ne servait plus qu'à charger du code.

## Décision

`provideZonelessChangeDetection()` dans `app.config.ts`, retrait du polyfill dans `angular.json` et de la
dépendance `zone.js`.

## Vérification

Build et 40 tests unitaires verts, puis les parcours rejoués dans Chromium : décompte après 429 (le compteur
descend), menu mobile, prédiction ouverte depuis un match, entraînement suivi jusqu'à la fin.

## Alternatives écartées

- **Garder zone.js** : aucun gain, un polyfill en plus, et des traces d'erreur plus longues.

## Avantages

- Moins de JavaScript chargé, et une détection des changements déclenchée seulement par les signals.
- Aligné avec la direction d'Angular.

## Inconvénients

- Une valeur modifiée hors signal (variable simple dans un `setTimeout`) ne s'affiche plus. Toute nouvelle page
  doit stocker son état dans des signals.
- Les bibliothèques tierces qui comptent sur zone.js ne sont pas compatibles ; aucune n'est utilisée aujourd'hui.
