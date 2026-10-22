# 0020 · Suppression du code hérité inutilisé

## Problème

La revue du backend (octobre 2026) a trouvé, à côté du code récent, une couche héritée qui n'était plus appelée ou
ne pouvait pas fonctionner :

- `SparkAnalyticsController` exposait 7 analyses Spark (`GET /api/v1/spark/analytics/*`) ouvertes à tout compte
  connecté. Chaque appel lançait un calcul Spark **synchrone** sur le fil HTTP, hors du verrou des données
  (ADR 0005) et des jobs (ADR 0009). Quelques appels en parallèle suffisaient à saturer la mémoire.
- Les 3 anciens entraînements (`POST /api/v1/spark/ml/train/random-forest`, `logistic-regression`,
  `score-prediction`) étaient synchrones et ignoraient aussi le verrou. `SparkMLPredictionService.predictGameOutcome`
  ne pouvait pas marcher (`createMatchFeatures` renvoyait `null`).
- `PredictionServiceImpl` : 408 lignes commentées dans un `@Service` vide ; `MatchServiceImpl` : 130 lignes
  commentées.
- Une vingtaine de services, DTO et dépôts jamais injectés, un `CacheService` qui vidait des caches inexistants
  (aucun `@EnableCaching`), des tâches planifiées désactivées dont les notifications n'étaient qu'un `TODO`.
- `/api/v1/statistics/*` doublait `/api/v1/stats/*` avec une enveloppe de réponse à part (`ApiResponse`).

Le frontend n'appelait aucune de ces routes.

## Décision

- Suppression du code ci-dessus, de `ApiResponse`, `ErrorCode`, `EmailService` (et de `spring-boot-starter-mail`),
  et du réglage `NBA_SCHEDULER_ENABLED`.
- Le client OpenAPI du frontend (`frontend/openapi/hoopmind-api.json`) est mis à jour : 15 routes en moins, aucune
  route modifiée.
- `AccessRulesTests` décrit la matrice des droits (public, utilisateur, staff, administrateur) route par route. Une
  route absente de la matrice fait échouer le test ; on vérifie aussi les 401, les 403 et les `@PreAuthorize`.

## Conséquences

- Le seul entraînement est le job `POST /api/v1/spark/ml/train/game-outcome/jobs` (ADR 0010), la seule prédiction
  le calcul Java (ADR 0013).
- Les entités `GameOdds`, `Leaderboard` et `UserPrediction` sont retirées du code, mais leurs tables restent en base
  (aucune migration destructive). Elles pourront être supprimées par un changeset Liquibase si elles restent vides.
- Une analyse qui mériterait de revenir serait refaite comme l'entraînement : job en arrière-plan, verrou des
  données, réservée aux administrateurs.
- Les modules `nba` et `ml` restent ouverts : `MatchPrediction` référence encore `Game` en JPA.

## Alternatives écartées

- **Protéger les analyses (`ADMIN`) sans les supprimer** : elles resteraient synchrones, hors verrou, et personne ne
  les utilise.
- **Garder le code commenté « pour mémoire »** : l'historique Git le conserve, et la section « Historique du projet »
  du README crédite ce travail.
