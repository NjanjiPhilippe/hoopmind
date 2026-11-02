# 0026 · Motif de révocation des jetons de rafraîchissement, délai de grâce entre onglets

## Problème

Un jeton de rafraîchissement révoqué puis présenté de nouveau était toujours pris pour un vol : toute la famille était
révoquée et l'audit enregistrait `REFRESH_TOKEN_REUSED`. Deux cas normaux déclenchaient pourtant cette alerte :

- **un jeton révoqué pour une autre raison qu'un échange** : déconnexion depuis un autre onglet, mot de passe changé,
  compte désactivé ou rôles modifiés. Le navigateur présente encore l'ancien cookie, et l'audit signale un vol qui
  n'en est pas un ;
- **deux onglets qui renouvellent la session au même moment** (restauration de session du navigateur, liens ouverts
  dans de nouveaux onglets). Le frontend partageait la requête entre les appels d'un même onglet, pas entre onglets :
  le second envoyait le jeton que le premier venait d'échanger, et l'utilisateur était déconnecté partout.

## Décision

- **Motif de révocation** (`refresh_tokens.revoked_reason`, migration 019) : `ROTATED` (échangé), `LOGOUT`,
  `ACCESS_CHANGED` (mot de passe, rôles, statut), `REUSE_DETECTED` (famille révoquée après une alerte).
- **Seul un jeton échangé (`ROTATED`) présenté de nouveau est un vol probable.** Les autres reçoivent un 401 simple,
  sans alerte ni révocation supplémentaire. Les jetons révoqués avant la migration n'ont pas de motif : ils sont traités
  comme échangés, ce qui garde le comportement d'avant.
- **Délai de grâce de 10 secondes** (`AuthService.ROTATION_GRACE`) : un jeton échangé il y a moins de 10 secondes est
  refusé (401) sans révoquer la famille. Aucun nouveau jeton n'est émis : le délai ne donne rien à un voleur.
- **Côté frontend**, le renouvellement passe par un verrou du navigateur (`navigator.locks`, nom `hoopmind-refresh`).
  Le cookie est lu à l'envoi : l'onglet qui attend présente le jeton que le premier vient de recevoir, et réussit.
  Le délai de grâce couvre les navigateurs sans ce verrou.
- **Une panne passagère ne ferme plus la session côté client** : seul un 401 ou un 403 au renouvellement la ferme.
  Après une erreur réseau, un 5xx ou un 429, le cookie reste valable et l'utilisateur n'est pas renvoyé à la connexion.

## Alternatives écartées

- **Délai de grâce qui renvoie un nouveau jeton** (comme certains fournisseurs OAuth) : un voleur qui rejoue le jeton
  dans les 10 secondes obtiendrait une session. Un refus simple suffit, puisque le verrou du navigateur évite la course.
- **Coordonner les onglets par `BroadcastChannel`** : il faut élire un onglet et lui faire relayer le jeton d'accès
  aux autres, beaucoup plus de code que le verrou, qui est disponible dans tous les navigateurs récents.
- **Supprimer les jetons au lieu de les révoquer** : on perdrait la détection de réutilisation, qui a besoin de
  retrouver le jeton échangé.

## Inconvénients assumés

- Un vrai vol rejoué dans les 10 secondes qui suivent l'échange légitime n'est pas signalé (il est quand même refusé).
- Le comportement dépend d'une colonne de plus à garder cohérente : toute nouvelle façon de révoquer un jeton doit
  donner son motif.
