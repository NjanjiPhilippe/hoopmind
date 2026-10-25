# 0022 · En début de saison, la forme part du bilan de la saison passée ; l'administrateur peut saisir les scores

## Problème

La forme d'une équipe (pourcentage de victoires, 10 derniers matchs, net rating) ne reposait que sur ses matchs de la
saison en cours joués avant la date du match. Conséquences :

- le jour de l'ouverture, aucune prédiction n'était possible (422 `NO_TEAM_FORM`) ;
- après un ou deux matchs, une équipe affichait 100 % ou 0 % de victoires, et le modèle la jugeait sur si peu ;
- sans import disponible (fichiers d'Eoin Moore introuvables, stats.nba.com bloqué), la saison en cours restait vide.

## Décision

- **Point de départ : la saison passée.** Pour chaque équipe, son bilan de la saison précédente (tous les matchs joués,
  playoffs compris : pourcentage de victoires, ratings offensif et défensif) compte comme **10 matchs** :
  - pourcentage de victoires : `(victoires + 10 × % saison passée) / (matchs joués + 10)` ;
  - 10 derniers matchs : les places vides prennent le % de la saison passée ;
  - ratings : moyenne pondérée, chaque match avec feuille de match pour 1, la saison passée pour 10.

  Après 10 matchs, la saison passée pèse encore pour moitié ; après 30, pour un quart. Une équipe absente de la saison
  passée part des valeurs neutres (50 %, 110), qui jouent le même rôle.
- **Même calcul à l'entraînement et en production.** `GameFeatureBuilder` (Spark) et `TeamForm`/`TeamPrior` (Java)
  appliquent les mêmes formules ; `JavaScoringParityTests` le vérifie match par match sur trois saisons simulées.
  L'entraînement charge la saison qui précède la première saison voulue, pour servir de point de départ, puis l'écarte.
- **Saisie des scores par l'administrateur.** `PUT /api/v1/games/{id}/result` crée ou corrige le score final d'un match
  et le marque comme terminé ; `DELETE` le retire. La page *Admin › Saisie des résultats* permet aussi d'ajouter un
  match absent du calendrier (`POST /api/v1/games`). Seuls les scores sont saisis : la forme est recalculée à chaque
  prédiction, il n'y a pas de moyenne stockée à tenir à jour.
- **Cache des explications.** La clé comprend maintenant les valeurs des facteurs : un score saisi ou importé change la
  forme, donc l'explication est régénérée.
- La prédiction n'est refusée (422) que si une équipe n'a joué ni cette saison avant la date, ni la saison passée.

## Conséquences

- **+** Prédictions possibles dès le premier jour, et moins extrêmes sur les premières semaines.
- **+** La saison en cours peut avancer sans aucun import.
- **−** Le modèle doit être **réentraîné** : les variables gardent leur nom mais pas leur calcul.
- **−** Les changements d'effectif (transferts, blessures) ne sont pas pris en compte : la saison passée décrit l'équipe
  de l'an dernier.
- **−** Un match saisi à la main n'a pas d'identifiant NBA : si un import arrive ensuite pour la même saison, il ajoute
  ses propres matchs. Supprimer alors les matchs saisis à la main (ou ne pas mélanger les deux sources dans une saison).
- **−** Sans feuille de match, un score saisi met à jour les victoires mais pas les ratings, qui restent sur la saison
  passée.
