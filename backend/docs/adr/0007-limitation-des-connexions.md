# 0007 · Limitation des tentatives de connexion

## Problème

`POST /auth/login` acceptait un nombre illimité d'essais. BCrypt ralentit chaque essai, mais un attaquant pouvait
quand même tester une liste de mots de passe sur un compte (force brute) ou un mot de passe courant sur beaucoup
de comptes (*password spraying*). L'audit les voyait passer sans rien pouvoir faire.

## Décision

- `auth.internal.LoginThrottle` tient deux compteurs d'échecs :
  - **identifiant + adresse IP** : 5 échecs, contre la force brute sur un compte ;
  - **adresse IP seule** : 50 échecs, tous comptes confondus, contre le *spraying*.
- Au seuil, l'adresse est bloquée **30 s**, puis le blocage double à chaque nouvel échec (1 min, 2 min…) jusqu'à
  **15 min**. Le compteur repart de zéro après 15 min sans échec, comptées **depuis la fin du blocage** : attendre la
  fin d'un blocage ne remet pas le compteur à zéro.
- Le contrôle a lieu **avant** la vérification du mot de passe : pendant un blocage, même le bon mot de passe est
  refusé, sinon le blocage ne protégerait rien.
- Réponse **429** au format ProblemDetail, avec le code `TOO_MANY_ATTEMPTS`, `retryAfterSeconds` et l'en-tête
  standard `Retry-After`. Le message ne dit pas si le compte existe.
- Une connexion réussie efface le compteur identifiant + IP, mais pas celui de l'IP.
- **Un seul** événement `LOGIN_THROTTLED` est écrit dans l'audit par blocage, pour qu'une attaque ne remplisse pas
  le journal.
- Les seuils sont réglables (`hoopmind.security.login-throttle.*`).

## Pourquoi ne pas bloquer le compte ?

Verrouiller un compte après N échecs donne à n'importe qui le moyen d'empêcher la victime de se connecter (déni de
service ciblé). Ici, c'est **l'adresse de l'attaquant** qui est freinée ; le propriétaire se connecte depuis chez
lui, ce que vérifie un test d'intégration.

## Alternatives écartées

- **Bucket4j ou Resilience4j RateLimiter** : utiles pour limiter un débit global. Ici, on veut compter des **échecs**
  par clé, avec un blocage qui grandit : quelques lignes de code testées suffisent, sans nouvelle dépendance.
- **Stockage Redis ou en base** : nécessaire avec plusieurs instances, mais c'est une infrastructure de plus pour
  un projet qui tourne en une instance.
- **CAPTCHA** : bonne protection complémentaire, mais elle touche l'UX et demande un service tiers.

## Avantages

- Une force brute en ligne devient impraticable : après 5 essais, on attend 30 s, puis 1 min, et ainsi de suite.
- Le front peut afficher un compte à rebours précis grâce à `Retry-After`.
- Pas de nouvelle dépendance. La mémoire est bornée (100 000 compteurs au plus, purge toutes les 5 min).

## Inconvénients et limites

- **En mémoire** : l'état est perdu au redémarrage et n'est pas partagé entre plusieurs instances.
- **L'adresse IP est `request.getRemoteAddr()`** : derrière un reverse proxy, tous les clients ont l'adresse du
  proxy. *Mise à jour (octobre 2026)* : le frontend est servi par nginx, qui relaie `/api`. Sans correction, 5 échecs
  sur un compte (tapés par n'importe qui) bloquaient ce compte pour tout le monde, et 50 échecs bloquaient toutes les
  connexions. `server.forward-headers-strategy: native` est donc activé : Tomcat ne lit `X-Forwarded-For` que pour
  une requête venue d'une adresse privée (le conteneur nginx), et nginx remplace l'en-tête au lieu de compléter
  celui du client. Le port du backend n'est publié que sur `127.0.0.1` pour qu'on ne puisse pas contourner nginx.
- Plusieurs utilisateurs derrière la même adresse (NAT d'entreprise) partagent le compteur « IP seule ». Le seuil
  de 50 laisse de la marge.
