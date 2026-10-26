# 0024 · Allègement du backend : routes inutilisées, stats.nba.com, module ML fermé

## Problème

Après l'ADR 0020, une deuxième passe (octobre 2026) a trouvé encore beaucoup de code que rien n'appelle :

- **Blessures** (`/api/v1/injuries`, 9 routes) et **rapports** (`/api/v1/reports`, 4 routes renvoyant des `Map`) : aucun
  écran ne les utilise, et le suivi des absents est réservé à la version privée.
- **Anciens pronostics** (`/api/v1/predictions`, `MatchPrediction`) : saisis à la main par un modérateur, jamais par
  le moteur de prédiction, et lisibles par n'importe quel compte (`GET /predictions/model/{model}` listait ceux de tous).
- **Filtres jamais appelés** : matchs par statut, par équipe, par période, du jour, en cours ; équipes par conférence,
  division, abréviation ; saison active ; joueurs par poste, par statut, par nom ; meilleurs marqueurs, passeurs,
  rebondeurs ; statistiques d'équipe hors classement ; changement de statut d'un match, d'une saison, d'un joueur.
- **Une interface `*Api` par contrôleur**, pour une seule implémentation, alors que le code récent annote directement
  ses contrôleurs ; des journaux « Fetching… » à chaque lecture ; la même conversion équipe → résumé copiée dans trois
  mappers ; une vingtaine de méthodes de dépôt jamais appelées.
- **Entités sans dépôt utilisé** : `PlayerGameStats`, `TeamGameStats` (la table est lue et écrite en JDBC), `MatchStatus`.
- **Import stats.nba.com** (ADR 0017) : depuis mars 2026 nba.com coupe tout client qui n'est pas un navigateur. Le
  bouton restait sur la page Données et échouait à chaque fois ; les fichiers d'Eoin Moore (ADR 0018) le remplacent.

## Décision

- Suppression du code ci-dessus (environ 4 000 lignes de Java et de tests), et des tables héritées vides
  `match_predictions`, `user_predictions`, `game_odds`, `leaderboards`, `injury_reports` (migration 017).
  `player_game_stats` reste : les feuilles de match des joueurs (Eoin Moore, `PlayerStatistics.csv`) y iront.
- Contrôleurs annotés directement, sans `ResponseEntity` quand le statut est fixe (`@ResponseStatus`). Les
  `operationId` et les tags OpenAPI sont gardés : le client généré du frontend ne change pas pour les routes restantes.
- `AppException.notFound(code, message)` remplace les blocs de cinq lignes ; une suppression charge l'entité (404 si
  absente) puis la supprime.
- Le module `ml` est fermé (Spring Modulith) : il ne lit les données NBA qu'à travers `GameHistory`, ce que
  `ModularityTests` vérifie désormais pour tout le module.
- La matrice des droits (`AccessRulesTests`) perd le niveau « staff » : plus aucune route n'est ouverte aux modérateurs.

## Conséquences

- L'API publique passe de 77 à 34 routes. Une route supprimée qui redeviendrait utile se réécrit en quelques lignes
  sur le modèle des routes restantes, l'historique Git garde l'ancienne version.
- Le rôle `MODERATOR` reste en base et dans l'administration des comptes, sans droit particulier pour l'instant.
- Les jobs `STATS_NBA_IMPORT` déjà enregistrés restent dans `background_jobs` mais ne sont plus lus.

## Alternatives écartées

- **Garder stats.nba.com en espérant un retour** : contourner l'empreinte TLS demanderait d'imiter un navigateur, ce
  qui va contre les conditions du site.
- **Garder les filtres « au cas où »** : chacun est une route publique de plus à tester et à documenter.
