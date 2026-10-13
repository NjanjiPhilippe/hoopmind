# 0016 · L'import lit aussi l'ancien format de la base de Wyatt Walsh

## Problème

L'import des matchs (`ingestion.nbadb`) a été écrit pour le schéma en étoile de nbadb (`dim_game`,
`fact_game_result`, `fact_team_game`, `fact_standings`), que la page Kaggle `wyattowalsh/basketball` annonce
depuis 2026. Mais l'archive réellement publiée (octobre 2026) ne contient pas ce schéma :

- `run_summary.json` montre que la dernière reconstruction complète (29/06/2026) a extrait 0 ligne, sans erreur ;
- `nba.duckdb` ne pèse que 12 Ko : une base vide ;
- `nba.sqlite` (2,35 Go) et le dossier `csv/` sont l'ancien format à plat (`game`, `team`, `line_score`…),
  qui s'arrête à la saison 2022-23.

Avec le seul fichier disponible, l'import échouait sur `no such table: dim_game`.

## Décision

`NbaDatabaseReader` devient une classe scellée à deux implémentations, choisies à l'ouverture d'après les tables
présentes :

- `NbadbReader` : le schéma nbadb, inchangé ;
- `LegacyKaggleReader` : l'ancien format, ramené aux mêmes lignes (`NbaDatabaseRows`). L'import, les bilans de
  saison et le modèle ne savent pas quel format a été lu.

Les écarts de l'ancien format sont compensés dans le lecteur :

| Écart | Traitement |
|---|---|
| Saison codée dans `season_id` (« 22022 ») | Convertie au format nbadb (« 2022-23 ») |
| Identifiants parfois stockés en nombre | Match normalisé sur dix caractères (« 0022200001 »), équipes en entier |
| Une ligne par match (domicile et extérieur) | Dépliée en une feuille de match par équipe |
| Points par période dans `line_score`, jusqu'à 10 prolongations | Lus à part et rattachés au match |
| `team_info_common` vide : ni conférence, ni division, ni classement | Alignement actuel de la NBA (inchangé depuis 2004-05) ; rangs recalculés au pourcentage de victoires |

Un fichier qui n'a ni `dim_game` ni `game` est refusé avec un message clair.

## Conséquences

- **+** L'import fonctionne avec le fichier réellement téléchargeable, sans attendre une nouvelle publication.
- **+** Le jour où Walsh publie le schéma nbadb, il est lu sans changement de code.
- **−** Les données s'arrêtent en 2022-23 : les prédictions portent sur ces saisons.
- **−** Les rangs recalculés ignorent les critères officiels de départage (confrontations directes, etc.) : deux
  équipes à égalité sont départagées au nombre de victoires puis à leur identifiant.
- **−** Avant 2004-05, l'alignement actuel est appliqué aux saisons anciennes (divisions différentes à l'époque).
  Sans effet avec les 10 saisons importées par défaut.
