# 0003 · L'import des données devient un module `ingestion`

## Problème

`nba.importer` mélangeait dans le module `nba` deux responsabilités : le **modèle de données NBA** (ce qu'est une
équipe, un match) et un **pipeline ETL** (téléchargement Kaggle, lecture d'une base SQLite tierce, CSV, jobs en
arrière-plan, endpoints d'administration). Toute modification de l'import touchait le module que lisent `ml` et le
front, et le module `nba` traînait des dépendances (client HTTP Kaggle, pilote SQLite) qui ne le concernent pas.

## Décision

- Nouveau module Spring Modulith **fermé** `ingestion` :
  - `ingestion.nbadb` : base SQLite (Walsh), téléchargement Kaggle, job d'import et son contrôleur ;
  - `ingestion.csv` : CSV de référence et leur contrôleur.
- Découpage **par source** et non par couche (`web`, `service`…) : chaque sous-paquet garde ses classes
  package-private, sans élargir leur visibilité.
- Les URL, propriétés de configuration et `operationId` ne changent pas.

## Alternatives écartées

- **Laisser l'import dans `nba`** : statu quo, mais le module ne pourrait jamais être fermé proprement.
- **Découper par couche** (`ingestion.web`, `ingestion.service`…) : obligerait à rendre publiques une quinzaine de
  classes aujourd'hui package-private, donc à élargir la surface interne.
- **Écrire via des services du module `nba`** plutôt qu'en JDBC : l'import insère des dizaines de milliers de lignes
  par lots ; passer par JPA entité par entité serait des ordres de grandeur plus lent.

## Avantages

- `nba` ne contient plus que le modèle et sa lecture ; personne n'appelle `ingestion` (vérifié par `modules.verify()`).
- L'import peut évoluer (nouvelle source, file d'attente, service séparé) sans toucher au reste.
- Prépare les corrections de sécurité suivantes (verrou unique, audit des imports) dans un périmètre clair.

## Inconvénients

- `ingestion` écrit dans les tables de `nba` : c'est un couplage au schéma, assumé et documenté, propre à un ETL.
  Les migrations Liquibase restent la référence commune.
- Renommage de paquets : l'historique `git blame` passe par `git log --follow`.
