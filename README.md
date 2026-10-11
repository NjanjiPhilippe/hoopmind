# HoopMind

Statistiques et prédictions NBA : une API Spring Boot 4 avec Apache Spark ML et une interface Angular 21.

| Dossier | Contenu |
|---|---|
| [`backend/`](backend/README.md) | API REST : Spring Boot 4.1, Spring Security 7 (JWT + jetons de rafraîchissement rotatifs), PostgreSQL, Liquibase, Spark 4.2 ML, Swagger réservé aux administrateurs |
| [`frontend/`](frontend/README.md) | Interface web : Angular 21 (signals, composants standalone), Tailwind CSS 4, client API généré depuis l'OpenAPI du backend |

## Démarrer

```bash
# Backend (Java 21, Docker)
cd backend
cp .env.example .env            # renseigner DB_PASSWORD, JWT_SECRET, HOOPMIND_ADMIN_PASSWORD
docker compose up -d postgres
set -a && . ./.env && set +a
./mvnw spring-boot:run          # http://localhost:8083

# Frontend (Node.js 22), dans un autre terminal
cd frontend
npm ci
npm start                       # http://localhost:4200
```

Le détail de l'architecture, de la sécurité et des choix techniques est dans le README de chaque dossier.

## Données

Les matchs viennent de la [NBA Basketball Database](https://www.kaggle.com/datasets/wyattowalsh/basketball) de **Wyatt Walsh**
(licence CC BY-SA 4.0) et les statistiques de saison de [NBA Stats (1947-present)](https://www.kaggle.com/datasets/sumitrodatta/nba-aba-baa-stats)
de **Sumitro Datta**. Elles ne sont pas incluses dans ce dépôt : elles se chargent depuis la page **Administration > Données**, voir [Charger les données NBA](backend/README.md#charger-les-données-nba).

## Historique du projet

HoopMind a été conçu et écrit par Saiko ([NjanjiPhilippe](https://github.com/NjanjiPhilippe)) entre novembre et décembre 2025 :
le modèle de données NBA, l'API REST, l'authentification, le pipeline Spark ML (préparation des données, forêt aléatoire,
régression logistique, prédiction de score) et l'interface Angular avec toutes ses pages.

En octobre 2026, avec l'aide de Claude (Anthropic), le projet a été migré vers Spring Boot 4.1, Spark 4.2 et Angular 21,
réorganisé en modules par fonctionnalité, doté d'une nouvelle gestion des utilisateurs et de la sécurité, puis rassemblé dans ce dépôt.
Les dates et le détail de chaque étape sont dans les sections « Historique du projet » de [`backend/`](backend/README.md#historique-du-projet)
et [`frontend/`](frontend/README.md#historique-du-projet).

## Droits

© 2025-2026 Saiko (NjanjiPhilippe). Tous droits réservés. Le code est publié pour consultation ; aucune licence d'utilisation,
de modification ou de redistribution n'est accordée.
