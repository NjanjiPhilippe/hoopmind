<!-- bmad:context -->
<!-- Vérifié le 2026-10-30 sur 608b2161d16551290d492e343fbd6997ad914ac7. Géré par bmad-project-context ; tout ce qui est entre ces marqueurs est remplacé au prochain rafraîchissement. Écrire ce qu'il faut garder hors des marqueurs. -->

## HoopMind

Statistiques et prédictions NBA, vitrine publique des compétences de Saiko (NjanjiPhilippe) pour des recruteurs ML/LLM. Monorepo : `backend/` (Java 21, Spring Boot 4.1, Spark 4.2 ML, PostgreSQL, Liquibase) et `frontend/` (Angular 21, Tailwind 4, Vitest). Les décisions sont dans `backend/docs/adr/` et `frontend/docs/adr/`, la planification BMAD dans `_bmad-output/`.

## Policy

- Jamais de push direct sur `main` : une branche par fonctionnalité ou par correction, puis une PR.
- Nommer les branches avec un préfixe conventionnel : `feature/`, `fix/`, `docs/`, `chore/`, `refactor/`, `test/`, suivi d'un nom court en kebab-case.
- Ce dépôt est public (open-core, tous droits réservés, sans licence libre). N'y mettre que ce qui sert la vitrine. Tout le reste va dans la version privée : la feature « joueurs absents », le recalcul des prédictions quand la lineup change, le pipeline Spark optimisé, les fonctionnalités payantes. En cas de doute, demander avant d'écrire du code.
- Aucun secret dans le dépôt : tout passe par des variables d'environnement documentées dans `backend/.env.example`.
- Ne jamais versionner les données Kaggle (`data/`) ni de photo réelle : les photos sont chargées par lien.
- Chaque décision technique a son ADR (problème, choix, alternatives écartées, inconvénients assumés), ajouté dans la même PR avec sa ligne dans le `README.md` du dossier `docs/adr/`.
- Écrire en français les commits, les PR, les ADR et les README ; le code (identifiants) reste en anglais.

## Where things are

- Modules backend (`com.np3tech.hoopmind.*`) : `user`, `auth`, `nba`, `ingestion`, `audit`, `ml`, `shared`. Leurs frontières sont vérifiées par `ModularityTests`.
- Tâches longues (import, entraînement) : jobs persistés de `shared/job` sous le verrou unique `shared/lock/DataOperationLock`.
- Imports de données : `ingestion/nbadb` (base Walsh, fichiers Eoin Moore) et `ingestion/csv` (CSV de Sumitro Datta).
- Prédiction : Spark entraîne (`ml/spark`), Java prédit (`ml/scoring`), le LLM explique (`ml/explanation`, port `PredictionExplainer`).
- Frontend : `core/` (auth, http, nba), `features/` (une page par dossier), `shared/components`.

## Running and verifying

- Backend : lancer `./mvnw -B verify` depuis `backend/`, comme la CI. Il faut Docker, car Testcontainers démarre PostgreSQL ; sans Docker, voir la commande de `backend/README.md#tests`.
- Frontend : lancer `npm test -- --watch=false` puis `npm run build` depuis `frontend/`. Un `npm test` seul démarre en mode surveillance.
- Le client API de `frontend/src/app/core/api/generated/` est généré et ignoré par git : ne jamais le modifier à la main.
- Après un changement de route ou de DTO dans le backend, mettre à jour `frontend/openapi/hoopmind-api.json` dans la même PR, avec `npm run api:update` (backend local lancé, `HOOPMIND_ADMIN_PASSWORD` défini). Sinon le frontend compile contre l'ancienne API.

## Conventions that differ from defaults

- Le schéma appartient à Liquibase (`ddl-auto: validate`) : toute modification passe par un nouveau fichier dans `backend/src/main/resources/db/changelog/changes/`, inclus dans `db.changelog-master.yaml`. Ne jamais modifier un changeset déjà fusionné.
- Toutes les erreurs HTTP suivent la RFC 9457 (`application/problem+json`) et ne révèlent ni chemin ni détail technique (ADR 0008).
- La liste des variables du modèle n'a qu'une source, `GameOutcomeFeatures.NAMES`. Toute variable ajoutée doit être calculée de la même façon en Spark et en Java, et `JavaScoringParityTests` doit rester vert (ADR 0013).
- Un port seulement aux vraies frontières techniques (moteur de prédiction, LLM). Le reste est du Spring classique par fonctionnalité. Pas de Spring AI : les adaptateurs LLM sont écrits à la main (ADR 0015).
- Frontend sans zone.js : tout état affiché vit dans un signal, et les composants sont `OnPush`.

## Known pitfalls

- Ne pas créer un nouveau système pour chaque nouvelle source ou nouveau modèle de données. Étendre l'existant : `ingestion` pour importer, `shared/job` et `DataOperationLock` pour les tâches longues, `AuditEvent` pour la traçabilité.

<!-- /bmad:context -->
