# 0017 · Les saisons récentes viennent de stats.nba.com

## Problème

La base de Wyatt Walsh réellement publiée sur Kaggle s'arrête à la saison 2022-23 (voir [ADR 0016](0016-ancien-format-kaggle.md)).
Il manque donc 2023-24, 2024-25, 2025-26 et la saison en cours : le modèle s'entraîne et prédit sur des données vieilles
de trois ans. L'archive de Sumitro Datta va plus loin, mais elle donne des statistiques de saison, pas des matchs.

## Décision

Un deuxième import, « Compléter avec stats.nba.com », lit les saisons manquantes à la source même de la base Walsh :
l'API publique de stats.nba.com, endpoint `leaguegamelog` (une ligne par équipe et par match joué, avec sa feuille de match).

- **Mêmes identifiants** de match (`0022400001`) et d'équipe (`1610612738`) que la base Walsh : l'import réutilise
  `NbaDatabaseImporter` et ses upserts, et une saison présente dans les deux sources est mise à jour sans doublon.
- **Une source commune** : `NbaSource` est l'interface que lisent l'import Walsh (`NbaDatabaseReader`) et
  `StatsNbaSource`. Alignement et classements recalculés sont partagés avec l'ancien format (`NbaAlignment`).
- **Saisons lues** : de la dernière saison en base (relue pour la compléter) à la saison en cours, qui commence en
  octobre. Deux requêtes par saison (saison régulière, playoffs), une seconde d'écart (`STATS_NBA_DELAY_MS`).
- **Tout est lu avant d'écrire** : l'écriture reste une seule transaction ([ADR 0014](0014-import-par-lots-et-coherence.md)),
  une coupure du site en cours de lecture ne laisse pas d'import partiel.
- **Job en arrière-plan** (`STATS_NBA_IMPORT`) sous le même verrou que l'import Walsh, journal d'audit dédié.
- **Résilience** : `@Retryable` sur délai dépassé, coupure, 429 et 5xx ([ADR 0011](0011-resilience-spring-7.md)) ;
  un refus (403) n'est pas retenté. Les en-têtes imitent un navigateur sur nba.com, sans quoi le site ne répond pas.

## Conséquences

- **+** Les matchs vont jusqu'à la saison en cours, sans compte ni clé, et se mettent à jour en relançant l'import.
- **+** Le code d'import, les bilans de saison et le modèle ne changent pas.
- **−** Le journal ne donne pas les points par quart-temps : ces matchs n'ont ni détail par période ni nombre de
  prolongations.
- **−** stats.nba.com n'a pas de contrat d'API : un changement de colonnes casserait l'import (les colonnes sont lues
  par leur nom, pas par leur position, et une réponse inattendue est signalée clairement).
- **−** Le site bloque souvent les adresses d'hébergeurs cloud : en production hébergée, l'import peut échouer. Il
  est prévu pour être lancé depuis un poste personnel, puis la base est partagée.
