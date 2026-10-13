# 0018 · Les saisons récentes viennent des fichiers d'Eoin Moore, play-in compris

## Problème

La base de Wyatt Walsh s'arrête à la saison 2022-23 ([ADR 0016](0016-ancien-format-kaggle.md)). Le complément
stats.nba.com ([ADR 0017](0017-complement-stats-nba.md)) ne fonctionne plus : depuis le 12 mars 2026, la protection
Akamai de nba.com vérifie l'empreinte TLS du client et coupe sans réponse tout ce qui n'est pas un navigateur
(issues #652 et #633 de nba_api). Il faut pourtant la saison 2023-24 pour la démo, dont la saison « en cours » est 2023-24.

Par ailleurs, l'import ignorait le tournoi de play-in (identifiants en 005, depuis 2020-21) : ces matchs manquaient
à l'historique.

## Décision

- **Source** : le jeu Kaggle d'Eoin Moore, *Historical NBA Data and Player Box Scores*
  (`eoinamoore/historical-nba-data-and-player-box-scores`), mis à jour chaque jour. Seuls `Games.csv` (un match par
  ligne) et `TeamStatistics.csv` (une feuille de match par équipe et par match, avec les points par quart-temps) sont lus.
  L'administrateur les dépose dans `EOINMOORE_DATA_DIR` (`data/eoinamoore` par défaut), comme `nba.sqlite`.
- **Mêmes identifiants** que la base Walsh : `EoinMooreSource` implémente `NbaSource` et passe par `NbaDatabaseImporter`.
  Les identifiants de match, stockés en nombre (`22300061`), sont remis sur dix caractères (`0022300061`) ; la saison
  vient de l'identifiant (les playoffs 2019-20 ont été joués à l'automne 2020). L'abréviation des équipes vient de
  l'identifiant de la franchise, l'alignement et les classements de `NbaAlignment`.
- **Saisons choisies** : de `fromYear` à `toYear` (dix au plus), par défaut la saison qui suit la dernière en base.
  Le fichier complet est parcouru, seules les saisons demandées restent en mémoire.
- **Play-in** : nouveau type de match `PLAY_IN`, importé par les trois sources (Walsh, ancien format, Eoin Moore). Il
  n'entre ni dans le bilan de saison régulière, ni dans les classements, ni dans la date de début des playoffs. Pour
  le modèle, c'est un match à élimination : `is_playoff` vaut 1.
- **Job en arrière-plan** (`EOIN_MOORE_IMPORT`) sous le même verrou que l'import Walsh, journal d'audit dédié.

## Conséquences

- **+** 2023-24 et les saisons suivantes s'importent sans réseau, avec les points par quart-temps et les prolongations.
- **+** Les saisons communes avec la base Walsh sont mises à jour sans doublon.
- **−** Les deux fichiers sont à télécharger à la main sur Kaggle (le jeu complet pèse près de 2 Go à cause du play-by-play).
- **−** Les points des prolongations au-delà de la deuxième sont regroupés dans la troisième : le fichier ne donne que
  les deux premières et le total.
- **−** Le bouton stats.nba.com reste en place mais échoue tant que nba.com bloque les clients non navigateurs.
