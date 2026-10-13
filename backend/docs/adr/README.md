# Décisions d'architecture (ADR)

Chaque fichier explique une décision : le problème constaté, ce qui a été choisi, les alternatives écartées,
les avantages et les inconvénients assumés. Les numéros suivent l'ordre de la revue des PR #1 à #3, puis l'ordre des chantiers suivants.

| N° | Décision | Statut |
|----|----------|--------|
| [0001](0001-port-modele-de-prediction.md) | Le contrôleur ML passe par un port `GameOutcomeModel`, plus par l'adaptateur Spark | Acceptée |
| [0002](0002-api-historique-nba.md) | Le module `ml` lit les matchs via l'API `GameHistory` du module `nba` | Acceptée |
| [0003](0003-module-ingestion.md) | L'import des données devient un module `ingestion` | Acceptée |
| [0004](0004-audit-par-evenements.md) | L'audit écoute des événements de domaine au lieu d'être appelé | Acceptée |
| [0005](0005-verrou-unique-des-donnees.md) | Un seul verrou pour l'import, les CSV et l'entraînement ; plus d'import synchrone | Acceptée |
| [0006](0006-audit-des-operations-sur-les-donnees.md) | Les imports et l'entraînement sont tracés dans l'audit | Acceptée |
| [0007](0007-limitation-des-connexions.md) | Limitation des tentatives de connexion | Acceptée |
| [0008](0008-messages-d-erreur.md) | Les messages d'erreur ne révèlent ni chemin ni détail technique | Acceptée |
| [0009](0009-jobs-persistes.md) | Jobs en arrière-plan persistés, état scellé, exécuteur de Spring | Acceptée |
| [0010](0010-entrainement-en-job.md) | L'entraînement du modèle devient un job | Acceptée |
| [0011](0011-resilience-spring-7.md) | `@Retryable` sur Kaggle ; pas de `@ConcurrencyLimit` | Acceptée |
| [0012](0012-variables-du-modele.md) | Le modèle n'utilise que des écarts entre équipes | Acceptée |
| [0013](0013-prediction-en-java.md) | Prédiction en Java, forme à la date du match, parité testée avec Spark | Acceptée |
| [0014](0014-import-par-lots-et-coherence.md) | Import par lots réécrits, une seule transaction, horodatages et rôles | Acceptée |
| [0015](0015-explication-par-llm.md) | Explication des prédictions par un LLM, avec nos propres adaptateurs plutôt que Spring AI | Acceptée |
| [0016](0016-ancien-format-kaggle.md) | L'import lit aussi l'ancien format de la base de Wyatt Walsh | Acceptée |
| [0017](0017-complement-stats-nba.md) | Les saisons récentes viennent de stats.nba.com | Acceptée |
