# 0013 · Prédiction en Java, forme à la date du match, parité testée avec Spark

## Problème

Trois défauts dans la façon de prédire un match :

1. **Lenteur.** Chaque prédiction rechargeait toute la saison dans Spark et exécutait le pipeline : 1 à 2 s par
   appel. Avec Spark désactivé, il n'y avait plus aucune prédiction.
2. **Écart entre entraînement et production** (*training/serving skew*). À l'entraînement, la forme d'une équipe
   était calculée *avant* chaque match, avec les vrais jours de repos et le drapeau playoffs. En prédiction,
   c'était la forme *de fin de saison*, avec un repos fixé à 2 jours et les playoffs toujours à faux. Le modèle
   recevait donc en production des entrées qu'il n'avait jamais vues.
3. **Aucune date.** Impossible de prédire un match passé « comme si on y était », ou un match de playoffs.

## Décision

- **Spark sert à entraîner, Java sert à prédire.** Après l'entraînement, le pipeline est exporté en **carte de
  score** (`ScoringCard`) : moyennes et écarts-types de la standardisation, poids et ordonnée à l'origine de la
  régression logistique. Elle est enregistrée en JSON (`<model-dir>/game-outcome-scoring-card.json`, écriture atomique).
- `ScoringGameOutcomePredictor` (sans Spark) :
  - lit les matchs des deux équipes via `GameHistory.teamGames` (une requête) ;
  - calcule leur forme **à la date du match** avec `TeamForm.before`, selon les mêmes règles qu'à l'entraînement
    (matchs précédents de la saison, 10 derniers, *net rating* sur les feuilles de match, repos plafonné à 7 jours) ;
  - applique la carte de score.
- `GET /spark/ml/predict/game` prend `date` (aujourd'hui par défaut) et `playoff` en options. Il renvoie un
  `GameOutcomePrediction` typé au lieu d'une `Map`, et fonctionne même quand Spark est coupé. Le front ne l'appelait pas encore.
- **Parité vérifiée par des tests**, sur 3 saisons simulées qui contiennent des matchs le même jour, des feuilles de
  match manquantes, des playoffs et des matchs à venir :
  - les 5 variables calculées en Java sont égales à celles de Spark **pour chaque match** (à 1e-9) ;
  - la probabilité de la carte de score est égale à celle du pipeline Spark **pour chaque match** (à 1e-9).
- La liste des variables a une **source unique** (`GameOutcomeFeatures.NAMES`), lue par Spark et par Java. Une carte
  dont les variables ne correspondent pas est ignorée (« modèle non entraîné ») plutôt qu'appliquée de travers.

Mesuré sur le serveur local : **10 à 15 ms** par prédiction (après le premier appel), contre 1 à 2 s auparavant.

## Alternatives écartées

- **Garder Spark pour prédire, avec un cache** : cela reste lent au premier appel, impose Spark en production pour
  servir, et ne corrige pas l'écart entre entraînement et production.
- **Exporter en PMML ou ONNX** : des formats standard, mais une bibliothèque d'exécution en plus pour une simple
  régression logistique (cinq multiplications et une sigmoïde).
- **Calculer la forme en SQL** : possible, mais il faudrait une troisième implémentation des règles à maintenir
  identique.

## Avantages

- Des prédictions instantanées, disponibles sans Spark.
- Les mêmes entrées en production qu'à l'entraînement, prouvé par les tests de parité.
- Prédire à une date passée permet un *backtest* honnête, et le drapeau playoffs est enfin pris en compte.

## Inconvénients

- Les règles de calcul de la forme existent en deux exemplaires (Spark et Java). Le test de parité est le garde-fou :
  toute divergence le fait échouer.
- Le modèle doit rester une régression logistique pour tenir dans une carte de score. Un autre type de modèle
  demandera un autre format d'export, derrière le même port `GameOutcomePredictor`.
- Le modèle Spark complet n'est plus enregistré (seule la carte l'est), ce qui suffit pour prédire et pour expliquer
  les poids.
