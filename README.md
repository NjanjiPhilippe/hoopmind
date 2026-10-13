# HoopMind

Statistiques et prédictions NBA : une API Spring Boot 4 avec Apache Spark ML et une interface Angular 21.
Chaque prédiction est expliquée en français par un LLM (Google Gemini gratuit, Groq, Ollama, OpenAI ou Claude),
qui met en mots les chiffres du modèle sans rien inventer.

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

## IA générative : explication des prédictions

Le modèle Spark donne une probabilité ; un LLM explique pourquoi, à partir du poids exact de chaque facteur
(forme, net rating, repos…). Il rédige, il ne prédit pas, et une explication sans LLM prend le relais s'il ne
répond pas. L'intégration passe par un port `PredictionExplainer` et deux adaptateurs écrits à la main (API
compatible OpenAI, API Anthropic) **plutôt que Spring AI** : le besoin se limite à un appel texte, ce qui évite une
dépendance de plus à aligner sur Spring Boot 4, garde le prompt et la gestion des erreurs visibles et testables, et
reprend l'architecture de [ledger-ai](https://github.com/NjanjiPhilippe/ledger-ai). Détails et configuration :
[Explication des prédictions par un LLM](backend/README.md#explication-des-prédictions-par-un-llm) et
[ADR 0015](backend/docs/adr/0015-explication-par-llm.md).

## Données

Les matchs viennent de la [NBA Basketball Database](https://www.kaggle.com/datasets/wyattowalsh/basketball) de **Wyatt Walsh**
(licence CC BY-SA 4.0) jusqu'à 2022-23, puis des [fichiers d'**Eoin Moore**](https://www.kaggle.com/datasets/eoinamoore/historical-nba-data-and-player-box-scores)
pour les saisons suivantes, et les statistiques de saison de [NBA Stats (1947-present)](https://www.kaggle.com/datasets/sumitrodatta/nba-aba-baa-stats)
de **Sumitro Datta**. Elles ne sont pas incluses dans ce dépôt : elles se chargent depuis la page **Administration > Données**, voir [Charger les données NBA](backend/README.md#charger-les-données-nba).

## Historique du projet

HoopMind a été conçu et écrit par Saiko ([NjanjiPhilippe](https://github.com/NjanjiPhilippe)) entre novembre et décembre 2025 :
le modèle de données NBA, l'API REST, l'authentification, le pipeline Spark ML (préparation des données, forêt aléatoire,
régression logistique, prédiction de score) et l'interface Angular avec toutes ses pages.

En octobre 2026, avec l'aide de Claude (Anthropic), le projet a été migré vers Spring Boot 4.1, Spark 4.2 et Angular 21,
réorganisé en modules par fonctionnalité, doté d'une nouvelle gestion des utilisateurs et de la sécurité, puis rassemblé dans ce dépôt.
L'explication des prédictions par un LLM a été ajoutée ensuite.
Les dates et le détail de chaque étape sont dans les sections « Historique du projet » de [`backend/`](backend/README.md#historique-du-projet)
et [`frontend/`](frontend/README.md#historique-du-projet).

## Droits

© 2025-2026 Saiko (NjanjiPhilippe). Tous droits réservés. Le code est publié pour consultation ; aucune licence d'utilisation,
de modification ou de redistribution n'est accordée.
