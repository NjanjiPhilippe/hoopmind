# 0027 · Compte de démonstration en lecture seule, limites par adresse IP

## Problème

HoopMind doit pouvoir s'essayer en ligne sans inscription : un recruteur ne crée pas de compte pour voir une
prédiction. Publier un identifiant et un mot de passe de démonstration ne tient pas :

- le premier visiteur qui change le mot de passe bloque tous les suivants ;
- n'importe qui peut modifier le profil, ou supprimer les prédictions que les autres visiteurs voient ;
- le quota d'appels au LLM est compté par utilisateur : sur un compte partagé, les vingt premières explications de
  l'heure épuisent le LLM pour tout le monde.

L'inscription elle-même était ouverte sans limite : un script pouvait créer des comptes en série pour multiplier
ce quota, et savoir au passage quelles adresses e-mail ont déjà un compte (« e-mail déjà utilisé »).

## Décision

- **Rôle `ROLE_DEMO`** (migration 020). Le compte de démonstration a `ROLE_USER` et `ROLE_DEMO` : il lit tout et
  prédit, mais `@PreAuthorize("!hasRole('DEMO')")` lui refuse (403) la modification du profil, le changement de mot
  de passe et la suppression de prédictions.
- **Connexion sans mot de passe** : `POST /api/v1/auth/demo` ouvre une session sur ce compte, une famille de jetons
  par visiteur. Le mot de passe du compte est aléatoire et jamais communiqué. `GET /api/v1/auth/demo` dit si la
  démonstration est ouverte, pour afficher le bouton « Essayer sans compte ».
- **Désactivée par défaut** (`DEMO_ENABLED=false`). Activée, elle crée le compte au démarrage (`demo` par défaut,
  `HOOPMIND_DEMO_USERNAME`). Un compte ordinaire du même nom n'est jamais repris : le démarrage échoue plutôt que
  d'ouvrir la session de quelqu'un.
- **Quota LLM par visiteur** : pour le compte de démonstration, le quota horaire est compté par compte et adresse IP.
- **Limites par adresse IP et par heure** : 5 inscriptions (`REGISTRATIONS_PER_ADDRESS_PER_HOUR`) et 30 sessions de
  démonstration (`DEMO_SESSIONS_PER_ADDRESS_PER_HOUR`). Au-delà : 429 avec `Retry-After`, comme le frein des
  connexions.

## Alternatives écartées

- **Publier un mot de passe de démonstration** : voir le problème.
- **Un compte temporaire par visiteur** : chaque visite ajouterait un compte à la base et à la liste des
  utilisateurs de l'administration, à purger ensuite.
- **Masquer l'existence d'un e-mail à l'inscription** : il faudrait une confirmation par e-mail pour que le message
  reste utile au vrai propriétaire. La limite par adresse rend l'énumération lente, ce qui suffit pour une vitrine.

## Inconvénients assumés

- Les visiteurs de la démonstration voient les prédictions des autres visiteurs dans « Mes prédictions » (gardées
  dans la limite de 200 par compte, voir l'ADR 0028).
- Les limites sont en mémoire et propres à l'instance, comme le frein des connexions. Derrière un proxy, nginx doit
  transmettre la vraie adresse du visiteur (voir `frontend/nginx.conf`), sinon tous les visiteurs partagent la même.
