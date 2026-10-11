# 0002 · Le module `ml` lit les matchs via l'API `GameHistory` du module `nba`

## Problème

`TeamGameLoader` (module `ml`) exécutait son propre SQL sur les tables `games`, `seasons`, `game_results` et
`team_game_stats`, qui appartiennent au module `nba`. `GameOutcomeModelService` utilisait aussi `SeasonRepository`.
Le module `ml` connaissait donc le schéma interne de `nba` : renommer une colonne ou changer la façon de stocker
les feuilles de match cassait la prédiction sans qu'aucun test de frontière ne le signale.

## Décision

- Le module `nba` publie une API en lecture :
  - `nba/GameRecord` : un match (saison, date, équipes, playoffs, score, et `BoxScore` des deux équipes) ;
  - `nba/GameHistory` : `recentSeasonYears(n)`, `games(saisons)`, `seasonYear(idSaison)`.
- `nba/history/JdbcGameHistory` l'implémente en **une seule requête JDBC** (même SQL qu'avant, déplacé chez son
  propriétaire), en `@Transactional(readOnly = true)`.
- Le calcul des possessions (`FGA − OREB + TOV + 0,44 × FTA`) devient une méthode de `BoxScore` : c'est une règle
  métier du basket, pas un détail de Spark.
- `TeamGameLoader` ne fait plus que convertir des `GameRecord` en lignes Spark.
- Une règle ArchUnit (`ModularityTests.predictionEngineReadsNbaOnlyThroughItsPublicApi`) interdit au moteur de
  prédiction de dépendre à nouveau des entités, dépôts ou services de `nba`.

## Pourquoi ne pas fermer les modules `nba` et `ml` tout de suite ?

Le code historique (`MatchPrediction → Game`, `SparkDataPreparationService`, mappers) a des relations JPA directes
entre les deux modules. Les fermer obligerait à réécrire ces entités dans la même PR. La règle ArchUnit protège le
nouveau code dès maintenant ; la fermeture viendra quand l'ancien moteur sera retiré.

## Alternatives écartées

- **Une vue SQL partagée** : garde le couplage au schéma, juste déplacé.
- **Des entités JPA `Game` exposées** : charge des graphes d'objets lourds pour des milliers de matchs, et expose
  l'entité mutable à un autre module.
- **Spark JDBC (`spark.read().jdbc`)** : plus rapide sur de très gros volumes, mais Spark lirait directement les
  tables de `nba`, exactement le couplage que l'on veut supprimer.

## Avantages

- Le schéma de `nba` peut évoluer sans casser `ml`, tant que `GameRecord` est rempli.
- Un record immuable, sans dépendance JPA ni Spark, facile à construire dans un test.
- La frontière est vérifiée automatiquement à chaque build.

## Inconvénients

- Toutes les lignes passent par la mémoire de la JVM avant Spark (comme avant : l'ancien code faisait déjà
  `jdbc.query` puis `createDataFrame`). Acceptable pour quelques saisons NBA (≈ 1 300 matchs par saison) ;
  à revoir si l'on entraîne sur 20 saisons ou plus.
- Une règle ArchUnit écrite par noms de classes : un nouveau fichier du moteur doit suivre la convention de nommage
  ou être ajouté à la règle.
