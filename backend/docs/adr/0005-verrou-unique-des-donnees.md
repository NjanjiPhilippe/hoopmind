# 0005 · Un seul verrou pour l'import, les CSV et l'entraînement

## Problème

La PR #3 a ajouté un job d'import en arrière-plan protégé par un `AtomicReference`, mais l'ancien endpoint
`POST /admin/imports/nba-database` (import **synchrone**) existait toujours et ne regardait pas ce verrou. Un
administrateur (ou un script) pouvait donc lancer deux imports en même temps. Les CSV et l'entraînement du modèle
n'étaient protégés par rien. Risques :

- deux transactions qui réécrivent les mêmes lignes (`games`, `team_game_stats`) : attentes de verrous,
  interblocage, ou import partiellement écrasé ;
- un modèle entraîné pendant qu'un import vide et remplit les tables, donc sur des données incohérentes ;
- une requête HTTP bloquée plusieurs minutes sur un fil du serveur, qui finit en timeout côté navigateur ou proxy.

## Décision

- Suppression de `POST /admin/imports/nba-database`. L'import ne se lance plus que par
  `POST /admin/imports/nba-database/jobs` (le front utilisait déjà celui-ci).
- L'import au démarrage (`NBA_IMPORT_ON_STARTUP`) passe lui aussi par le job.
- `shared.lock.DataOperationLock` : un verrou unique, non lié à un fil, que prennent l'import Walsh, l'import CSV
  et l'entraînement. Une deuxième opération reçoit un **409 `DATA_OPERATION_RUNNING`** qui dit laquelle est en
  cours (« un import de la base NBA est en cours »).
- Le verrou se rend dans un `try-with-resources`. Rendre deux fois le même jeton ne libère jamais l'opération
  suivante, ce qui est testé.

## Alternatives écartées

- **Verrou PostgreSQL (`pg_advisory_lock`) ou ShedLock** : fonctionne avec plusieurs instances, mais ajoute une
  dépendance et une connexion tenue pendant toute l'opération. Inutile tant que l'API tourne en une seule instance.
  Le passage aux jobs persistés (étape Java/Spring) rendra l'état visible en base.
- **Un verrou par opération** : l'import CSV et l'import Walsh écrivent dans des tables voisines, et l'entraînement
  lit ce que les deux écrivent. Les séparer ne ferait que déplacer les conflits.
- **File d'attente (on met la 2ᵉ opération en attente)** : plus confortable, mais l'administrateur ne sait plus
  quand son action démarrera. Un refus explicite est plus lisible pour un outil d'administration.

## Avantages

- Une seule règle simple : une opération lourde sur les données à la fois.
- Plus aucun fil HTTP bloqué par un import.
- Le message d'erreur dit pourquoi l'action est refusée.

## Inconvénients

- Verrou en mémoire : avec deux instances de l'API, chacune aurait le sien. C'est documenté, et ça se règlera
  avec un verrou en base si l'on passe à plusieurs instances.
- Endpoint supprimé : un script externe qui l'appelait reçoit maintenant 404/405 (le client Angular ne l'utilisait pas).
- Un entraînement (encore synchrone à cette étape) bloque un import pendant sa durée. C'est voulu.
