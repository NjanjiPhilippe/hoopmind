# 0021 · API NBA : services qui reçoivent les requêtes, pagination, horloge, limite LLM

## Problème

La revue du backend (octobre 2026) a relevé, dans le code NBA hérité :

- **Couches mélangées** : le contrôleur faisait construire une entité par un *mapper*, qui appelait lui-même des
  services pour charger les équipes et la saison. La mise à jour passait par une « fausse » entité à moitié remplie,
  et `createGame` lisait le nom des équipes avant de vérifier qu'elles existaient. `updateGame` ignorait
  `isNationalTv` et `broadcastNetwork`.
- **Une interface par service** pour une seule implémentation, alors que le code récent s'en passe.
- **Listes publiques sans limite** : `GET /api/v1/games` renvoyait tous les matchs (plus de 12 000 avec dix saisons),
  sans compte.
- **Joueurs et blessures illisibles** : la réponse est construite hors transaction (`open-in-view: false`), et
  l'équipe du joueur n'était pas chargée. Dès qu'un joueur existait, `/api/v1/players` répondait 500
  (`LazyInitializationException`).
- **Heure non maîtrisée** : `LocalDateTime.now()` dans les services, impossible à tester à une date donnée.
- **Modification concurrente** (`@Version`) : 500 au lieu de 409.
- **Quota gratuit du LLM** : un seul compte pouvait le vider en changeant la date du match à chaque appel.

## Décision

- Les services reçoivent la requête (`GameCreateRequest`…) et chargent eux-mêmes ce qu'elle référence. Les mappers ne
  font plus que construire les réponses. Les interfaces à une seule implémentation sont fusionnées dans leur classe.
- `GET /games`, `/games/status/{status}`, `/games/team/{teamId}`, `/players`, `/players/position/{position}` et
  `/players/status/{status}` renvoient une page (`PagedModel`, 20 par défaut, 100 au plus via
  `spring.data.web.pageable.max-page-size`). `/games/date-range` est limité à 31 jours.
- Les lectures de joueurs et de blessures chargent leurs associations (`@EntityGraph`), comme celles des matchs.
- Les services NBA et de pronostics reçoivent le `Clock` de l'application (UTC).
- `OptimisticLockingFailureException` donne un 409 `concurrent-update`.
- Chaque utilisateur a droit à 20 appels au LLM par heure (`hoopmind.ml.explanation.max-calls-per-user-per-hour`). Au
  delà, il reçoit l'explication rédigée sans LLM (`fallback = true`) : il n'est jamais bloqué. Une explication déjà
  en cache ne compte pas.
- L'interface web de Spark (port 4040) est désactivée.

## Conséquences

- Le frontend n'utilise aucune des routes devenues paginées : le client OpenAPI est régénéré sans changement de code.
- La limite LLM est en mémoire, comme le frein des connexions (ADR 0007) : perdue au redémarrage, propre à l'instance.
- Les entités gardent leurs horodatages `@PrePersist` en `LocalDateTime` : les passer en `Instant` demande une
  migration des colonnes, à faire avec le reste du schéma hérité.

## Alternatives écartées

- **Renvoyer 429 au-delà de la limite LLM** : l'utilisateur perdrait la prédiction, alors que l'explication sans LLM
  suffit.
- **Paginer aussi `/games/season/{seasonId}`** : le calendrier du frontend charge une saison entière (environ 1 300
  matchs), ce qui reste borné.
