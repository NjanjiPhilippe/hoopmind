# 0001 · La page Données suit l'entraînement comme un job

## Problème

Le backend lance l'entraînement en arrière-plan depuis l'ADR 0010 du backend, mais la page Données appelait encore
l'ancien endpoint synchrone, gardé seulement pour la transition. Le bouton restait figé pendant toute la durée de
l'entraînement, sans indication d'avancement, et un rechargement de la page perdait le résultat. La page laissait
aussi passer des saisies invalides (0 saison, 2,5 saisons) que le serveur refusait ensuite, et elle affichait
un modèle à 58 % sans dire qu'une règle simple faisait 67 %.

## Décision

- Le bouton appelle `POST /train/game-outcome/jobs`. Une ressource lit `GET /train/game-outcome/status`, et
  `pollWhile` la relit chaque seconde tant que la phase est `TRAINING`. À l'ouverture de la page, un entraînement
  déjà en cours est repris tel quel.
- Le rapport affiché est celui du job s'il vient de réussir, sinon celui du modèle en service.
- Les nombres de saisons sont vérifiés avant l'appel (entier, de 1 à 30 pour l'entraînement, de 1 au maximum
  annoncé par le serveur pour l'import).
- Quand le modèle fait moins bien que la meilleure des deux règles de référence, un avertissement le dit en points
  de précision (« 8,3 points de précision en moins ») et propose d'importer plus de saisons.
- Les poids des variables sont triés par importance et nommés en clair (« Écart de forme sur 10 matchs »).
- L'endpoint synchrone `POST /train/game-outcome` est **supprimé** du backend, comme prévu par l'ADR 0010.

## Alternatives écartées

- **Garder l'appel synchrone avec un indicateur de chargement** : la requête reste exposée aux délais des proxys,
  et le résultat se perd si l'onglet est fermé.
- **Server-Sent Events** : demande un second canal côté serveur pour une action lancée à la main, rarement.
- **Laisser le serveur seul valider les saisies** : il le fait déjà, mais l'utilisateur ne voit l'erreur qu'après
  un aller-retour.

## Avantages

- Une seule manière de suivre une opération longue sur la page, pour l'import comme pour l'entraînement.
- Le résultat survit à un rechargement et l'état « en cours » est visible par tous les administrateurs.
- La page ne présente plus un modèle médiocre comme un bon modèle.

## Inconvénients

- Une requête par seconde pendant l'entraînement (négligeable pour une action d'administrateur).
- La suppression de l'endpoint synchrone casse tout client qui l'appelait encore ; il n'y en a pas d'autre que
  cette page.
