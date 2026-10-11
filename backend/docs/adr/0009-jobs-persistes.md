# 0009 · Jobs en arrière-plan persistés, état scellé, exécuteur de Spring

## Problème

Le job d'import de la PR #3 gardait son état dans un `AtomicReference` et tournait sur un `Thread.ofVirtual()` lancé
à la main.

- **Après un redémarrage, l'état était perdu.** La page Données ne savait plus qu'un import avait eu lieu, ni qu'il
  avait été coupé en plein milieu.
- **Le fil n'était pas géré par Spring.** À l'arrêt de l'application, rien n'attendait sa fin, et la base pouvait
  rester à moitié écrite sans que personne le sache.
- **L'état était un record avec une `phase` et des champs facultatifs.** Rien n'empêchait un état incohérent, par
  exemple `SUCCEEDED` avec une erreur, ou `FAILED` sans message.
- **`AsyncConfig` déclarait son propre `ThreadPoolTaskExecutor`** (5 à 10 fils). Cela désactivait l'exécuteur de
  Spring Boot, donc les fils virtuels que `spring.threads.virtual.enabled` était censé activer pour `@Async`.

## Décision

- `shared.job.JobService` :
  - enregistre chaque job dans une table `background_jobs` (Liquibase 012) avec son type, l'auteur capturé au
    lancement, les paramètres, l'étape courante, le résultat en JSONB et l'erreur ;
  - prend le verrou des données de son type (ADR 0005) **avant** d'écrire la ligne : un refus 409 ne laisse aucune trace ;
  - exécute le travail sur l'**exécuteur de Spring Boot** (`applicationTaskExecutor`, fils virtuels) ;
  - au démarrage, marque **« interrompu »** tout job resté « en cours », puisque le processus qui l'exécutait n'existe plus.
- `JobState<R>` est une **interface scellée** : `Running`, `Succeeded(result)`, `Failed(error)` et
  `Interrupted`. Chaque état ne porte que ses propres données, et un `switch` sans `default` force à traiter
  chaque cas (l'API d'import et celle d'entraînement le font).
- L'avancement fin, comme les octets téléchargés, reste en mémoire : il change plusieurs fois par seconde. Seuls les
  changements d'étape vont en base.
- Arrêt propre : `spring.task.execution.shutdown.await-termination: true`, avec 30 s pour finir.
- `AsyncConfig` ne déclare plus d'exécuteur, ce qui rend les fils virtuels à `@Async`.
- L'API garde la même forme (`JobStatus`) et gagne un `id` ainsi que la phase `INTERRUPTED`.

## Alternatives écartées

- **Spring Batch** : il apporte relance, reprise sur erreur et historique complet, mais aussi une dizaine de tables
  et un modèle (job, step, chunk) disproportionné pour deux traitements.
- **JobRunr ou Quartz** : bons outils de planification, mais une dépendance et un tableau de bord de plus, alors
  qu'aucune planification n'est nécessaire ici.
- **Garder l'état en mémoire et ajouter juste un `@PreDestroy`** : moins de code, mais l'état reste perdu après un
  plantage, ce qui est justement le cas qu'on veut voir.

## Avantages

- Après un redémarrage, l'administrateur voit le dernier import ou entraînement, réussi ou interrompu.
- Un seul mécanisme pour toutes les opérations longues : verrou, persistance, audit et arrêt propre.
- Les états impossibles ne compilent pas.
- Les tâches `@Async` (e-mails) tournent enfin sur des fils virtuels.

## Inconvénients

- Une table de plus, sans purge pour l'instant (une ligne par import ou entraînement : volume négligeable).
- Avec plusieurs instances, « interrompu au démarrage » marquerait les jobs d'une autre instance encore vivante. Il
  faudrait alors un propriétaire et un battement de cœur par job. C'est documenté, car l'API tourne en une seule instance.
- Un job coupé n'est pas relancé automatiquement : l'administrateur le relance. C'est voulu, pour qu'un import
  interrompu ne reparte pas tout seul à chaque démarrage.
