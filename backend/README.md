# HoopMind · backend

API de statistiques et de prédictions NBA : Spring Boot 4, PostgreSQL, Apache Spark ML.

## Démarrer en local

Prérequis : Java 21, Docker (pour PostgreSQL et les tests).

```bash
cp .env.example .env                     # puis renseigner DB_PASSWORD, JWT_SECRET, HOOPMIND_ADMIN_PASSWORD
docker compose up -d postgres            # base PostgreSQL 17
set -a && . ./.env && set +a
./mvnw spring-boot:run                   # API sur http://localhost:8083
```

- `JWT_SECRET` : `openssl rand -base64 48`
- Au premier démarrage, un administrateur est créé à partir de `HOOPMIND_ADMIN_*` s'il n'en existe aucun.
- Spark démarre en mode `local[*]` dans la JVM. `SPARK_ENABLED=false` démarre l'API sans Spark ;
  `docker compose --profile spark-cluster up -d` lance un cluster Spark 4.2 (avec `SPARK_MASTER=spark://spark-master:7077`).
- Tout en conteneurs : `docker compose --profile app up -d --build`.

## Documentation de l'API

Swagger UI : http://localhost:8083/swagger-ui.html, réservé aux administrateurs (identifiants HoopMind en HTTP Basic).
Pour appeler les routes protégées depuis Swagger, obtenir un jeton avec `POST /api/v1/auth/login` puis cliquer sur **Authorize**.
La documentation est désactivée en profil `prod`.

## Tests

```bash
./mvnw verify
```

Les tests d'intégration démarrent PostgreSQL avec Testcontainers et passent par toute la chaîne (HTTP, sécurité, JPA, Liquibase).
Sans Docker : `./mvnw verify -Dhoopmind.test.testcontainers=false -Dspring.datasource.url=jdbc:postgresql://localhost:5432/hoopmind_test -Dspring.datasource.username=... -Dspring.datasource.password=...`.

`ModularityTests` vérifie les frontières entre modules avec Spring Modulith et génère leur documentation dans `target/spring-modulith-docs`.

## Architecture

Monolithe modulaire, découpé par fonctionnalité (`com.np3tech.hoopmind.*`) :

| Module | Rôle |
|---|---|
| `user` | Comptes, profils, rôles, administration des utilisateurs. API publique : `UserAccountService` et l'événement `UserCredentialsRevoked`. |
| `auth` | Connexion, jetons, configuration Spring Security (API et Swagger). Dépend de `user`, jamais l'inverse. |
| `nba` | Équipes, joueurs, saisons, matchs, statistiques, blessures. |
| `ml` | Prédictions et analyses. Le reste de l'application passe par le port `GameOutcomePredictor` ; l'implémentation Spark vit dans `ml.spark`. |
| `shared` | Configuration commune, erreurs, e-mail, audit. |

**Choix assumés**

- *Monolithe modulaire plutôt que microservices* : un seul déployable, des frontières vérifiées par les tests (Spring Modulith).
- *Hexagonal ciblé* : un port uniquement là où il y a une vraie frontière technique (le moteur de prédiction, demain un LLM). Le reste est du Spring classique par fonctionnalité, plus simple à lire.
- `nba` et `ml` sont déclarés « ouverts » : le code ML existant utilise directement les entités NBA. Ils seront refermés lors du chantier ML.
- Le schéma appartient à Liquibase ; Hibernate se contente de le valider (`ddl-auto: validate`).
- Toutes les erreurs suivent la RFC 9457 (`application/problem+json`).

### Sécurité

- Jeton d'accès JWT (HS256, 15 min) signé et vérifié par Spring Security OAuth2 Resource Server ; les rôles sont dans la claim `roles`.
- Jeton de rafraîchissement opaque (7 jours) dans un cookie `HttpOnly`, `SameSite=Strict`, limité à `/api/v1/auth`. Seule son empreinte SHA-256 est stockée.
- Rotation à chaque usage, avec détection de réutilisation : rejouer un ancien jeton révoque toute la session.
- Changer de mot de passe, être désactivé, changer de rôles ou être supprimé révoque immédiatement toutes les sessions de l'utilisateur.
- Mots de passe hachés en bcrypt (`DelegatingPasswordEncoder`, migration d'algorithme possible).
- Aucun secret dans le dépôt : tout passe par des variables d'environnement (`.env.example`).

### Routes principales

| Méthode | Route | Accès |
|---|---|---|
| POST | `/api/v1/auth/register`, `/login`, `/refresh`, `/logout` | public |
| GET, PATCH | `/api/v1/users/me` | connecté |
| PUT | `/api/v1/users/me/password` | connecté |
| GET | `/api/v1/admin/users?search=&page=&size=` | ADMIN |
| GET, DELETE | `/api/v1/admin/users/{id}` | ADMIN |
| PUT | `/api/v1/admin/users/{id}/status`, `/roles` | ADMIN |
| GET | `/api/v1/teams`, `/players`, `/games`, `/seasons`, `/stats`… | public |
| * | `/api/v1/spark/**` | connecté (entraînement : ADMIN) |

## Versions

Java 21 · Spring Boot 4.1.1 · Spring Security 7.1 · Hibernate 7.4 · Liquibase 5 · springdoc-openapi 3.1 · Spring Modulith 2.1 ·
Apache Spark 4.2.0 (Scala 2.13) · PostgreSQL 17 · Testcontainers 2.

Spark 4.2 est aligné sur Spring Boot 4.1 (Jackson 2.21, Jetty 12.1, Netty 4.2, ANTLR 4.13), ce qui règle le conflit ANTLR qui faisait planter Spark avec Hibernate.
Spark garde Jackson 2 alors que Spring Boot 4 utilise Jackson 3 : les deux cohabitent sans conflit (paquets différents).

## Historique du projet

Le socle fonctionnel a été conçu et écrit par Saiko (NjanjiPhilippe) entre novembre et décembre 2025 :

| Date | Travail |
|---|---|
| 19/11/2025 | Création du projet Spring Boot |
| 21/11/2025 | Configuration Docker (PostgreSQL, cluster Spark) |
| 26/11/2025 | Modèle de données NBA (19 tables), schéma Liquibase, entités, dépôts, services et contrôleurs REST, authentification JWT et sécurité par rôles |
| 10/12/2025 | Préparation des tests |
| 13/12/2025 | Corrections et configuration Docker |

Ce socle comprend notamment toute la partie NBA (équipes, joueurs, saisons, matchs, statistiques, blessures, rapports) et le pipeline Spark ML
(préparation des données, forêt aléatoire, régression logistique, prédiction de score, analyses).

En octobre 2026, avec l'aide de Claude (Anthropic) :

- migration vers Spring Boot 4.1 et Spark 4.2, mise à jour de toutes les dépendances ;
- réorganisation en modules par fonctionnalité, vérifiés par Spring Modulith ;
- réécriture de l'authentification (Resource Server, jetons de rafraîchissement rotatifs) et gestion des utilisateurs (profil, administration) ;
- Swagger réservé aux administrateurs ;
- secrets sortis du dépôt, erreurs au format RFC 9457, tests d'intégration sur PostgreSQL, intégration continue.
