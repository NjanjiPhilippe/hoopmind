# 0010 · L'entraînement du modèle devient un job

## Problème

`POST /spark/ml/train/game-outcome` entraînait le modèle **dans la requête HTTP** : 26 s sur 224 matchs simulés, et
plusieurs minutes sur dix saisons réelles. Le navigateur, un proxy ou l'utilisateur pouvaient abandonner avant la
fin. Un fil du serveur restait occupé pendant tout ce temps, et un double clic lançait deux entraînements.

## Décision

- `POST /spark/ml/train/game-outcome/jobs` répond **202** tout de suite avec l'identifiant du job.
  `GET /spark/ml/train/game-outcome/status` donne l'état du dernier entraînement : en cours, réussi avec son rapport,
  en échec avec le message, ou interrompu.
- L'entraînement passe par `JobService` (ADR 0009). Il a donc le même verrou que les imports (409 si l'un d'eux
  tourne), la même persistance et le même audit (`MODEL_TRAINED`, ADR 0006).
- `ml.training.ModelTrainingJobs` ne dépend que du port `GameOutcomeModel` : sans Spark, il répond 503.
- **Transition** : l'ancien `POST /train/game-outcome` est gardé, marqué **déprécié** dans OpenAPI. Il lance le même
  job et attend sa fin (10 min au plus). L'interface actuelle continue donc de fonctionner sans modification ; elle
  passera au suivi du job pendant le chantier frontend, puis l'ancien endpoint sera supprimé.

## Alternatives écartées

- **Supprimer l'endpoint synchrone tout de suite** : plus propre, mais cela casse la page Modèle avant le chantier
  frontend, que tu as demandé de faire après le backend.
- **`@Async` qui renvoie un `CompletableFuture`** : la requête attend quand même, et l'état n'est ni persisté ni partagé.
- **Server-Sent Events pour pousser l'avancement** : agréable, mais le *polling* toutes les secondes, déjà utilisé
  par la page Données, suffit pour une opération lancée à la main.

## Avantages

- Plus de requête HTTP de plusieurs minutes, ni d'entraînement en double.
- Le résultat survit à une fermeture d'onglet ou à un redémarrage.
- Le même modèle d'interaction pour l'import et l'entraînement, ce qui simplifie le frontend à venir.

## Inconvénients

- Deux endpoints pour la même action pendant la transition.
- Le frontend doit gérer un état « en cours » (chantier frontend).

## Suite

La page Données suit maintenant le job (ADR 0001 du frontend) ; l'endpoint synchrone a été supprimé.
