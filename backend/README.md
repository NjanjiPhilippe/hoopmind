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
- Les tâches planifiées sur les matchs (statuts, rappels, statistiques) sont désactivées par défaut : `NBA_SCHEDULER_ENABLED=true` les active.
- Spark démarre en mode `local[*]` dans la JVM. `SPARK_ENABLED=false` démarre l'API sans Spark ;
  `docker compose --profile spark-cluster up -d` lance un cluster Spark 4.2 (avec `SPARK_MASTER=spark://spark-master:7077`).
- Tout en conteneurs : `docker compose --profile app up -d --build`.

### Charger les données NBA

Les données ne sont pas fournies avec le dépôt (voir [Données NBA](#données-nba)). Tout se fait depuis la page
**Administration > Données** du frontend :

1. **Matchs et résultats** : renseignez `KAGGLE_USERNAME` et `KAGGLE_KEY` dans `.env` (jeton créé sur kaggle.com, Settings,
   API, Create New Token), puis cliquez sur « Télécharger depuis Kaggle et importer ». Le serveur télécharge la base de
   Wyatt Walsh lui-même (plusieurs Go, sans passer par le navigateur) puis l'importe en arrière-plan ; la page suit l'avancement.
2. **Statistiques de saison et trophées** : déposez le zip de l'archive de Sumitro Datta, ou des CSV isolés, dans n'importe
   quel ordre. Chaque fichier est reconnu par ses colonnes, quel que soit son nom, et la page affiche un compte rendu par fichier.
3. **Modèle** : choisissez le nombre de saisons et lancez l'entraînement ; la page affiche la précision, la comparaison aux
   règles simples et le poids de chaque variable.

Sans interface, les mêmes opérations existent dans l'API (jeton administrateur) :

```bash
scripts/download-nba-database.sh         # ou téléchargement manuel de data/nba.sqlite
curl -X POST "localhost:8083/api/v1/admin/imports/nba-database?seasons=10" -H "Authorization: Bearer $TOKEN"
curl -X POST "localhost:8083/api/v1/admin/imports/csv" -F "files=@archive.zip" -H "Authorization: Bearer $TOKEN"
curl -X POST "localhost:8083/api/v1/spark/ml/train/game-outcome?seasons=5" -H "Authorization: Bearer $TOKEN"
```

`NBA_IMPORT_ON_STARTUP=true` lance l'import au démarrage. Chaque import est rejouable : il met à jour les lignes existantes sans créer de doublon.

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
| `audit` | Journal d'audit : qui a fait quoi, sur quel compte, avec quel résultat. Les autres modules publient un `AuditEvent`. |
| `ml` | Prédictions et analyses. Le reste de l'application passe par le port `GameOutcomePredictor` ; l'implémentation Spark vit dans `ml.spark`. |
| `shared` | Configuration commune, erreurs, e-mail. |

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
- Journal d'audit des actions sensibles (voir ci-dessous).
- Aucun secret dans le dépôt : tout passe par des variables d'environnement (`.env.example`).

### Journal d'audit

Sont tracés : inscriptions, connexions et échecs de connexion, déconnexions, réutilisation d'un jeton de rafraîchissement,
modifications de profil, changements (et échecs de changement) de mot de passe, création de l'administrateur initial,
et toutes les actions des administrateurs sur les comptes (activation, désactivation, rôles, suppression), y compris celles refusées.

- Chaque événement garde l'auteur, le compte concerné, la date, le résultat et un motif. Les noms sont copiés pour rester
  lisibles après la suppression d'un compte.
- Pas d'adresse IP, pas de mot de passe, pas de valeur saisie : un profil modifié ne garde que le nom des champs, et une
  connexion avec un identifiant inconnu n'enregistre pas cet identifiant (il peut contenir un mot de passe tapé dans le mauvais champ).
- Une réussite n'est enregistrée que si l'action est validée ; un échec l'est toujours. L'écriture se fait dans une transaction
  séparée et ne peut pas faire échouer l'action.
- Les événements sont aussi écrits dans le journal applicatif `hoopmind.audit`, et supprimés après `AUDIT_RETENTION` (365 jours par défaut).
- Consultation : `GET /api/v1/admin/audit-events` (filtres `action`, `outcome`, `userId`, `user`, `from`, `to`) et la page *Administration → Journal d'audit*.

### Routes principales

| Méthode | Route | Accès |
|---|---|---|
| POST | `/api/v1/auth/register`, `/login`, `/refresh`, `/logout` | public |
| GET, PATCH | `/api/v1/users/me` | connecté |
| PUT | `/api/v1/users/me/password` | connecté |
| GET | `/api/v1/admin/users?search=&page=&size=` | ADMIN |
| GET, DELETE | `/api/v1/admin/users/{id}` | ADMIN |
| PUT | `/api/v1/admin/users/{id}/status`, `/roles` | ADMIN |
| GET | `/api/v1/admin/audit-events?action=&outcome=&userId=&user=&from=&to=` | ADMIN |
| GET | `/api/v1/teams`, `/players`, `/games`, `/seasons`, `/stats`… | public |
| POST | `/api/v1/admin/imports/nba-database?seasons=` (synchrone) | ADMIN |
| GET | `/api/v1/admin/imports/nba-database/status` | ADMIN |
| POST | `/api/v1/admin/imports/nba-database/jobs?seasons=&download=` (arrière-plan) | ADMIN |
| POST | `/api/v1/admin/imports/csv` (multipart `files`), GET `/csv/datasets` | ADMIN |
| POST | `/api/v1/spark/ml/train/game-outcome?seasons=` | ADMIN |
| GET | `/api/v1/spark/ml/model-metrics`, `/ml/predict/game` | connecté |
| * | `/api/v1/spark/**` | connecté (entraînement : ADMIN) |

## Données NBA

| Source | Contenu | Utilisation |
|---|---|---|
| [NBA Basketball Database](https://www.kaggle.com/datasets/wyattowalsh/basketball) de **Wyatt Walsh** ([nbadb](https://github.com/wyattowalsh/nbadb)), licence CC BY-SA 4.0 | Tous les matchs depuis 1946-47 avec scores, feuilles de match, classements, issus de stats.nba.com | Équipes, saisons, matchs, résultats, statistiques par match et bilans de saison |
| [NBA Stats (1947-present)](https://www.kaggle.com/datasets/sumitrodatta/nba-aba-baa-stats) de **Sumitro Datta**, d'après Basketball-Reference | Statistiques de saison des joueurs et des équipes, votes des trophées individuels | Importée telle quelle (table `reference_csv_rows`), pour la future prédiction des trophées (MVP, DPOY, ROY…) |

Les données ne sont pas versionnées : chacun les télécharge depuis Kaggle, et `data/` est ignoré par git.
Merci à Wyatt Walsh et à Sumitro Datta pour leur travail de collecte et de publication.

L'import (`nba.importer`) lit le fichier `nba.sqlite` en lecture seule et ne garde que la saison régulière et les playoffs
des `NBA_IMPORT_SEASONS` dernières saisons (10 par défaut). Il calcule ensuite le bilan de chaque équipe par saison
(victoires, domicile et extérieur, séries, ratings offensif et défensif, rythme) à partir des feuilles de match.

L'import des CSV (`nba.importer.csv`) reconnaît les 22 fichiers de l'archive par leurs colonnes caractéristiques et range
chaque ligne sous sa clé naturelle (par exemple saison, ligue, joueur et équipe), avec ses valeurs en JSON : « NA » devient nul,
les nombres restent des nombres. Une ligne déjà présente n'est réécrite que si elle a changé.

## Modèle de prédiction des matchs

Une régression logistique Spark ML prédit la probabilité de victoire de l'équipe à domicile.

- **Variables** : pour chaque équipe, sa forme *avant* le match (bilan, dix derniers matchs, ratings offensif, défensif et net,
  jours de repos) et les écarts entre les deux équipes. Elles sont calculées avec des fenêtres Spark
  (`rowsBetween(unboundedPreceding, -1)`) : le modèle ne voit jamais le résultat qu'il doit prédire,
  contrairement à un bilan de fin de saison qui le contient déjà.
- **Évaluation dans le temps** : le modèle apprend sur les saisons les plus anciennes et il est testé sur la plus récente.
  Le nombre de saisons se règle avec `ML_TRAINING_SEASONS` (5 par défaut) ou `?seasons=`, pour comparer la précision selon l'historique utilisé.
- **Références** : la précision est comparée à deux règles simples sur les mêmes matchs, « l'équipe à domicile gagne » et
  « l'équipe au meilleur bilan gagne ». Les métriques (précision, AUC, log loss, matrice de confusion, poids des variables)
  sont enregistrées à chaque entraînement et servies par `GET /api/v1/spark/ml/model-metrics`.
- Le modèle est enregistré dans `ML_MODEL_DIR` et rechargé au redémarrage.

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
- import des données NBA de Wyatt Walsh et modèle de prédiction des matchs évalué sur une saison réservée ;
- page « Données » : téléchargement de la base Walsh par le serveur depuis Kaggle, dépôt des CSV de Sumitro Datta reconnus automatiquement, entraînement du modèle.
