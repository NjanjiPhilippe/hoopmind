# Décisions d'architecture (ADR)

Chaque fichier explique une décision : le problème constaté, ce qui a été choisi, les alternatives écartées,
les avantages et les inconvénients assumés. Les numéros suivent l'ordre de la revue des PR #1 à #3.

| N° | Décision | Statut |
|----|----------|--------|
| [0001](0001-port-modele-de-prediction.md) | Le contrôleur ML passe par un port `GameOutcomeModel`, plus par l'adaptateur Spark | Acceptée |
| [0002](0002-api-historique-nba.md) | Le module `ml` lit les matchs via l'API `GameHistory` du module `nba` | Acceptée |
| [0003](0003-module-ingestion.md) | L'import des données devient un module `ingestion` | Acceptée |
| [0004](0004-audit-par-evenements.md) | L'audit écoute des événements de domaine au lieu d'être appelé | Acceptée |
