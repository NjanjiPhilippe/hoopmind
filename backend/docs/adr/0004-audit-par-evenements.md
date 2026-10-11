# 0004 · L'audit écoute des événements de domaine au lieu d'être appelé

## Problème

Avec la PR #1, `user` et `auth` construisaient eux-mêmes des `AuditEvent` (`AuditAction`, `AuditActor`,
`AuditTarget`) : les modules métier dépendaient du module d'audit. C'est l'inverse du sens attendu : l'audit est un
observateur. Concrètement :

- chaque nouvelle règle d'audit (format, nouvelle action) obligeait à modifier `user` ou `auth` ;
- `AuditActor.current()` lisait la claim JWT `username` : le module d'audit connaissait le format du jeton émis par `auth` ;
- impossible de retirer ou remplacer l'audit sans toucher aux modules métier.

## Décision

- `user` publie `UserAccountEvent`, une **interface scellée** de records : `Registered`, `ProfileUpdated`,
  `PasswordChanged`, `PasswordChangeRejected`, `EnabledChanged`, `RolesChanged`, `Deleted`, `AdminBootstrapped`,
  `LastAdminProtected`.
- `auth` publie `AuthenticationEvent` (scellée) : `LoginSucceeded`, `LoginFailed` (avec une raison typée),
  `LoggedOut`, `RefreshTokenReused`.
- Chaque événement dit s'il est un échec (`failed()`). `audit.internal.AuditTranslator` écoute :
  - les réussites en `AFTER_COMMIT` (rien n'est tracé si la transaction est annulée) ;
  - les échecs en `AFTER_COMPLETION` (tracés même si l'exception annule la transaction).
- La traduction se fait avec un `switch` sur type scellé, **sans branche `default`** : ajouter un événement sans le
  traduire ne compile pas.
- `auth.CurrentUser` est le seul endroit qui lit le jeton. `AuditEvent`, `AuditActor` et `AuditTarget` deviennent
  internes au module d'audit.
- Le contenu du journal est identique (mêmes actions, raisons et détails) : les tests `AuditTrailTests` passent
  sans modification.

## Alternatives écartées

- **Aspect AOP `@Audited`** : invisible à la lecture du code métier, et un aspect ne sait pas distinguer « refusé
  parce que dernier admin » d'une autre exception sans recréer ces informations.
- **Spring Modulith `@ApplicationModuleListener` + registre de publications** : garantit la livraison même après un
  crash, mais il est asynchrone et ne trace que les transactions validées ; on perdrait les échecs annulés
  (connexion refusée, dernier admin) et l'utilisateur connecté. Envisageable plus tard pour les événements de données.
- **Un événement générique `AuditableEvent(action, …)` dans `shared`** : déplace simplement le couplage.

## Avantages

- `user` et `auth` ne connaissent plus l'audit ; le graphe des modules n'a plus que des flèches « métier ← audit ».
- Les événements décrivent ce qui s'est passé dans le vocabulaire du métier ; d'autres écouteurs (notifications,
  statistiques) peuvent s'y abonner sans modifier les émetteurs.
- Le compilateur vérifie l'exhaustivité de la traduction (types scellés + pattern matching de Java 21).

## Inconvénients

- Un type d'événement par fait métier : plus de fichiers qu'un appel direct.
- L'auteur de l'action est résolu au moment de l'écoute (même fil d'exécution que la requête). Si un jour les
  écouteurs deviennent asynchrones, il faudra mettre l'auteur dans l'événement.
- `audit` dépend maintenant de `user` et de `auth` (sens voulu, mais à garder en tête pour éviter un cycle).
