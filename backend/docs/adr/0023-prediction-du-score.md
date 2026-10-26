# 0023 · Prédire le score : deux régressions linéaires sur les moyennes de points

## Problème

Le modèle ne donnait que le vainqueur et sa probabilité. Un fan veut aussi un score : combien de points pour chaque
équipe, et de combien l'une bat l'autre. Les données disponibles sont les résultats match par match (scores, feuilles de
match d'équipe), sans play-by-play ni statistiques de joueurs.

## Décision

- **Deux régressions linéaires Spark ML**, une pour les points de l'équipe à domicile, une pour ceux de l'équipe à
  l'extérieur. L'écart et le total s'en déduisent. Elles partagent le même assembleur et la même standardisation, dans un
  seul pipeline.
- **Variables** (`GameScoreFeatures`) : points marqués et encaissés par match de chaque équipe avant le match, écart de
  jours de repos, playoffs. Contrairement au modèle du vainqueur, qui ne voit que des écarts, chaque équipe garde ses
  valeurs : le total dépend du niveau absolu (deux attaques à 120 points ne donnent pas le même total que deux attaques
  à 100). L'avantage du terrain est porté par les ordonnées à l'origine.
- **Début de saison** : comme le reste de la forme ([ADR 0022](0022-forme-de-debut-de-saison.md)), les moyennes de
  points mêlent les matchs de la saison et la moyenne de la saison passée, qui compte comme 10 matchs.
  `JavaScoringParityTests` vérifie que Spark et Java calculent les mêmes valeurs, et que la carte de score donne les
  mêmes points que le pipeline Spark.
- **Pas d'entraînement séparé.** Le job d'entraînement existant entraîne les deux modèles sur les mêmes matchs, avec le
  même découpage dans le temps, et leur donne la même version. Le score est appliqué en Java, comme le vainqueur
  (`game-score-card.json` à côté de `game-outcome-scoring-card.json`).
- **Évaluation** sur la saison de test, enregistrée dans `prediction_model_metrics` (ligne `game-score-linear-regression`) :
  erreur moyenne sur l'écart (MAE et RMSE), sur les points d'une équipe, sur le total, part des matchs où l'écart désigne
  le bon vainqueur, et erreur d'une règle simple (« chaque équipe marque la moyenne de ce qu'elle marque et de ce que
  l'adversaire encaisse », sans avantage du terrain).
- **Exposition** : `score` dans la réponse de `GET /predict/game` et `/predict/game/explanation` (points arrondis sans
  jamais de nul, écart et total avant arrondi, erreur moyenne du modèle, moyennes de points des deux équipes) ; `score`
  dans `GET /model-metrics`. L'explication (LLM ou sans LLM) cite le score estimé comme une estimation.
- **Monitoring** : `hoopmind.ml.predicted.margin` répartit les écarts prédits (en valeur absolue, par `model_version`),
  à côté de `hoopmind.ml.home.win.probability` ([ADR 0019](0019-monitoring.md)).

## Conséquences

- **+** Aucune donnée nouvelle à importer, aucun service de plus : un réentraînement suffit pour activer le score.
- **+** Chaque prédiction affiche son incertitude (erreur moyenne sur l'écart), et l'administrateur voit si le modèle bat
  la règle simple.
- **−** Les deux modèles sont indépendants : quand le match est serré, le score peut donner l'avantage à l'équipe qui
  n'est pas favorite. La page et l'explication le disent (« match très serré ») au lieu de forcer l'un à suivre l'autre.
- **−** Tant que le modèle n'a pas été réentraîné, `score` vaut `null` et la page n'affiche que le vainqueur.
- **−** Une régression linéaire ne connaît ni les blessures ni le rythme de jeu au-delà de ce que les moyennes de points
  en reflètent. Une erreur moyenne d'une dizaine de points sur l'écart est l'ordre de grandeur attendu en NBA avec ces
  seules données.
