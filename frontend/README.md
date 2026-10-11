# HoopMind · frontend

Interface web de HoopMind : Angular 21, Tailwind CSS 4.

## Démarrer en local

Prérequis : Node.js 22, le backend HoopMind lancé sur http://localhost:8083.

```bash
npm ci
npm start          # http://localhost:4200, /api est relayé vers le backend (proxy.conf.json)
```

| Commande | Rôle |
|---|---|
| `npm start` | serveur de développement |
| `npm test` | tests unitaires (Vitest) |
| `npm run build` | build de production dans `dist/` |
| `npm run api:generate` | régénère le client API depuis `openapi/hoopmind-api.json` (lancé automatiquement avant `start`, `build` et `test`) |
| `npm run api:update` | récupère la spécification du backend local puis régénère le client (`HOOPMIND_ADMIN_PASSWORD` requis) |

## Architecture

```
src/app/
├── core/
│   ├── api/generated/   client HTTP généré depuis l'OpenAPI du backend (non versionné)
│   ├── auth/            session, intercepteur, guards
│   └── http/            lecture des erreurs RFC 9457
├── shared/components/   composants d'interface réutilisables
└── features/            une fonctionnalité par dossier, chargée à la demande
    ├── auth/            connexion, inscription
    ├── account/         profil et mot de passe
    ├── admin/           tableau de bord et gestion des utilisateurs
    └── ...              pages NBA (équipes, joueurs, matchs, prédictions, statistiques)
```

- **Composants standalone, organisés par fonctionnalité** et chargés à la demande par le routeur.
- **Signals** pour l'état (session, formulaires, listes avec `resource`), composants `OnPush`.
  Zone.js reste actif tant que les pages NBA existantes n'ont pas été converties ; le nouveau code est prêt pour le mode zoneless.
- **Client API généré** (ng-openapi-gen) : les types et les routes viennent du backend, ce qui évite qu'ils divergent.
- **Session** : le jeton d'accès reste en mémoire, jamais dans le `localStorage`. Le jeton de rafraîchissement est un cookie `HttpOnly`
  posé par le backend ; au chargement, l'application rouvre la session avec lui. Sur une réponse 401, l'intercepteur renouvelle
  la session une fois (les appels simultanés partagent la même requête) puis rejoue la requête.
- **Même origine** : en développement le proxy Angular, en production nginx (`nginx.conf`) relaient `/api` vers le backend. Pas de CORS à gérer.

## Docker

```bash
docker build -t hoopmind-frontend .
```

L'image sert l'application avec nginx et relaie `/api` vers l'hôte `backend:8083`.

## Historique du projet

L'interface a été conçue par Saiko (NjanjiPhilippe) en novembre 2025 :

| Date | Travail |
|---|---|
| 17/11/2025 | Création du projet Angular 18 |
| 19/11/2025 | Pages et design Tailwind : accueil, connexion, inscription, dashboard, équipes, joueurs, matchs, prédictions, statistiques, administration ; services et modèles |
| 21/11/2025 | Configuration nginx et Docker |

En octobre 2026, avec l'aide de Claude (Anthropic) :

- migration vers Angular 21 (18 → 19 → 20 → 21 avec les migrations officielles), nouveau système de build, Vitest ;
- passage à Tailwind CSS 4 (configuration en CSS) ;
- client API généré depuis l'OpenAPI du backend ;
- nouvelle gestion de session (jeton en mémoire, cookie HttpOnly, renouvellement automatique) ;
- pages de compte (profil, mot de passe) et d'administration des utilisateurs ;
- page d'administration « Données » : import des matchs depuis Kaggle, dépôt des CSV, entraînement et résultats du modèle.
