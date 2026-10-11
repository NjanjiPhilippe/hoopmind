# 0012 · Le modèle n'utilise que des écarts entre équipes

## Problème

Le modèle recevait 18 variables. Pour chaque statistique, il avait la valeur de l'équipe à domicile, celle de
l'équipe à l'extérieur, **et** leur écart (`home_x`, `away_x`, `x_diff = home_x − away_x`). Il avait aussi
`point_diff_diff` en plus de `net_rating_diff`, qui mesure presque la même chose. Ces variables sont
**colinéaires** : la régression peut répartir le même effet entre elles d'une infinité de façons.

- Les poids étaient instables d'un entraînement à l'autre et **illisibles**. Sur les données de test,
  `net_rating_diff` et `point_diff_diff` recevaient exactement le même poids (0,136), et `home_win_pct` avait un
  poids quasi nul alors que le bilan est la variable la plus prédictive.
- Le graphique « poids des variables » de la page Modèle racontait donc n'importe quoi à un recruteur.

## Décision

Cinq variables, toutes des écarts entre l'équipe à domicile et l'équipe à l'extérieur :
`win_pct_diff`, `last10_win_pct_diff`, `net_rating_diff`, `rest_days_diff` et `is_playoff`.
L'avantage du terrain est porté par l'**ordonnée à l'origine** : l'équipe « home » reçoit toujours.

Mesuré sur les données simulées des tests (3 saisons, test sur la dernière) :

| | 18 variables | 5 écarts |
|---|---|---|
| Précision | 0,740 | **0,752** |
| AUC | 0,764 | **0,768** |
| Log loss | 0,548 | **0,539** |
| Poids | dispersés, doublons | `net_rating_diff` 0,58 > `win_pct_diff` 0,22 > forme récente 0,13 ≈ repos 0,12 |

## Alternatives écartées

- **Garder les 18 variables et augmenter la régularisation (L2)** : cela stabilise les poids, mais sans les rendre
  lisibles, et cache le problème au lieu de le corriger.
- **Garder les valeurs brutes et retirer les écarts** : c'est équivalent pour le modèle, mais deux fois plus de
  poids à expliquer, sans gain.

## Avantages

- Des poids interprétables, stables, et qui racontent quelque chose de vrai sur le basket.
- Un modèle un peu meilleur et plus simple, donc plus facile à appliquer en Java (ADR 0013).

## Inconvénients

- Le modèle suppose que seul l'**écart** compte : deux équipes à 0,7 ou deux équipes à 0,3 sont traitées pareil.
  Avec un seul modèle linéaire, c'est un compromis raisonnable. Un modèle non linéaire (forêts, *boosting*) pourra
  réintroduire les niveaux absolus.
- Les anciens rapports d'entraînement listent des variables qui n'existent plus. La page Modèle les affiche quand
  même grâce au libellé générique.
