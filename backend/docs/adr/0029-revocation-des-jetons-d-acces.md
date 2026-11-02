# 0029 · Les jetons d'accès d'un compte désactivé ou modifié sont refusés tout de suite

## Problème

Désactiver un compte, changer ses rôles ou le supprimer révoquait ses jetons de rafraîchissement, mais pas son jeton
d'accès : un JWT sans état reste valable jusqu'à son expiration (15 minutes), rôles compris. Un compte administrateur
compromis gardait donc la console pendant 15 minutes après avoir été désactivé, alors que l'événement s'appelle
`UserCredentialsRevoked`.

## Décision

- **Une version de révocation par compte** : `RevokedAccessTokens` l'augmente à chaque `UserCredentialsRevoked`
  (désactivation, changement de rôles, suppression), et chaque jeton d'accès porte la version de son compte au moment
  de son émission (claim `rev`). Le décodeur JWT refuse (401) un jeton d'une version antérieure ; un jeton émis après
  la révocation, à la connexion suivante, est accepté.
- **Le changement de mot de passe n'est pas concerné** : il déconnecte les autres sessions (jetons de
  rafraîchissement), et l'utilisateur qui vient de changer son mot de passe garde la sienne.

## Alternatives écartées

- **Relire le compte en base à chaque requête** : une requête SQL de plus par appel, pour un cas rare.
- **Raccourcir les jetons à 5 minutes** : réduit la fenêtre sans la fermer, et triple les renouvellements.
- **Comparer la date d'émission du jeton à celle de la révocation** : la date d'un JWT est à la seconde, et ne
  départage pas un jeton émis juste avant la révocation d'un jeton émis juste après dans la même seconde.
- **Une colonne en base** : survit aux redémarrages, mais un jeton émis avant un redémarrage expire de toute façon
  dans les 15 minutes ; la mémoire suffit pour une seule instance.

## Inconvénients assumés

- L'état est en mémoire : juste après un redémarrage, un jeton émis avant une révocation antérieure au redémarrage
  reste accepté jusqu'à son expiration.
- Une entrée par compte révoqué reste en mémoire jusqu'au redémarrage : l'oublier plus tôt ferait accepter de nouveau,
  après une deuxième révocation, les jetons émis entre les deux.
