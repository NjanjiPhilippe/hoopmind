# 0008 · Les messages d'erreur ne révèlent ni chemin ni détail technique

## Problème

Les erreurs d'import renvoyées à l'interface contenaient le chemin absolu du fichier sur le serveur
(`/app/data/nba.sqlite`) et le message brut des exceptions SQL ou d'entrée/sortie. Même si ces écrans sont réservés
aux administrateurs, ces messages :

- dévoilent l'arborescence et la technologie du serveur, ce qui est utile à un attaquant si un compte admin est
  compromis ou si une réponse fuit (captures d'écran, journaux du navigateur) ;
- sont incompréhensibles pour l'utilisateur, qui ne sait pas quoi faire d'un `SQLITE_CORRUPT`.

## Décision

- Les messages montrés à l'administrateur disent **quoi faire** : « Base NBA introuvable sur le serveur.
  Téléchargez-la depuis la page Données… », « Fichier illisible : CSV mal formé ou encodage autre que UTF-8 ».
- Le détail technique (chemin, exception, trace) part dans les **journaux du serveur**, au niveau `WARN` ou `ERROR`.
- Règle générale : seuls les messages des `AppException`, écrits pour l'utilisateur, sont affichés tels quels.
  Toute autre exception devient « Erreur inattendue. Le détail est dans les journaux du serveur. »
- Une exception à la règle, utile à l'utilisateur : le dépassement de taille d'un envoi garde son message
  (« 512 Mo au plus »), grâce à un type d'exception dédié.
- Un test vérifie que le message d'un fichier manquant ne contient pas le chemin du serveur.

## Avantages

- Moins d'informations exploitables exposées.
- Des messages qu'un administrateur peut comprendre et suivre.

## Inconvénients

- Pour diagnostiquer une erreur inattendue, il faut lire les journaux du serveur. Une étape suivante pourra ajouter
  un identifiant de corrélation dans la réponse pour retrouver la bonne ligne.
- La page Données affiche toujours le chemin **configuré** du fichier, sciemment : c'est un réglage que
  l'administrateur doit connaître, pas une fuite d'erreur.
