# 0001 · Un port `GameOutcomeModel` entre le web et Spark

## Problème

Après la PR #2, `SparkAnalyticsApi` (couche API du module `ml`) importait `GameOutcomeModelService`, une classe
du paquet `ml.spark`, et son record imbriqué `TrainingReport`. La couche « ce que l'application sait faire »
dépendait donc de la couche « comment Spark le fait ». Conséquences concrètes :

- le contrôleur était `@ConditionalOnSpark` : avec Spark coupé, les endpoints d'entraînement et de métriques
  disparaissaient (404) sans explication ;
- changer de moteur (scoring en Java pur, ONNX, service Python) obligeait à toucher le contrôleur et le contrat OpenAPI ;
- les tests web devaient charger une session Spark.

## Décision

- `ml/GameOutcomeModel` : interface (port) avec `train(seasons)` et `latestReport()`.
- `ml/TrainingReport` : record autonome, sans type Spark, devenu le contrat du rapport d'entraînement.
- `ml/spark/GameOutcomeModelService` implémente le port (adaptateur).
- `ml/web/GameOutcomeModelController` reprend les deux endpoints avec **les mêmes chemins, tag et `operationId`** :
  le client Angular généré ne change pas. Il reçoit un `ObjectProvider<GameOutcomeModel>` et répond
  `503 MODEL_ENGINE_DISABLED` quand aucun moteur n'est actif, au lieu d'empêcher le démarrage.

## Alternatives écartées

- **Garder le contrôleur `@ConditionalOnSpark` branché sur le service Spark** (situation de départ) : moins de code,
  mais un 404 est plus difficile à diagnostiquer qu'un 503 explicite, et le couplage reste.
- **Une interface par cas d'usage (`TrainModel`, `ReadModelMetrics`)** : plus « pur », mais deux interfaces pour une seule
  implémentation n'apportent rien aujourd'hui.

## Avantages

- Dépendances dans le bon sens (web → port ← adaptateur), vérifiables par test.
- Le moteur de prédiction devient remplaçable sans toucher au contrat HTTP (prépare la décision « scoring en Java »).
- Sans Spark, les endpoints existent toujours et répondent une erreur explicite (503) au lieu d'un 404.

## Inconvénients

- Un fichier et une indirection de plus.
- `TrainingReport` duplique une partie de l'entité `ModelMetrics` (choix assumé : le contrat d'API ne doit pas
  suivre les colonnes de la table).
