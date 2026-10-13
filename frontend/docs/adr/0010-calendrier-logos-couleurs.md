# 0010 · Calendrier par équipe, logos et couleurs des franchises

## Problème

Pour prédire un match, il fallait choisir deux équipes, une saison et un jour à la main, puis comparer soi-même avec
le vrai résultat. La page Matchs filtrait déjà par équipe, mais du plus récent au plus ancien, sans bilan ni lecture
du point de vue de l'équipe. Toutes les équipes avaient la même couleur : on distinguait mal les deux camps d'une
prédiction.

## Décision

- **Agenda du mois** (`app-month-calendar`), comme Google Agenda : une grille du lundi au dimanche, flèches pour le
  mois précédent ou suivant (bornées aux mois qui ont des matchs). Avec une équipe, chaque case montre son match
  (logo de l'adversaire, « vs » ou « @ », V ou D et le score, fond vert ou rouge) et un clic choisit ce match. Sans
  équipe, chaque case montre les affiches du jour et un clic choisit le jour, dont les matchs sont listés dessous.
- **Calendrier** (ancienne page Matchs, même URL `/games`) : l'agenda du mois, ouvert sur le mois du prochain match
  de l'équipe, puis la liste des matchs de ce mois. Un clic ouvre la prédiction du match.
- **Prédire part du calendrier** : la page propose d'abord « Choisir un match » dans le même agenda, soit par équipe
  (son prochain match est proposé et prédit aussitôt), soit par jour. La saisie libre de deux équipes et d'un jour
  reste possible, repliée sous « Ou imaginer une autre affiche ».
- **Résultat réel** : le lien du calendrier porte l'identifiant du match (`game`). La page Prédire lit ce match et,
  s'il a été joué, affiche le score et si le favori du modèle a gagné. Changer une équipe, la saison ou le jour fait
  oublier ce match.
- **Couleurs** : les couleurs officielles des trente franchises sont dans le code (`team-branding.ts`), par
  abréviation. Quand les deux équipes d'un match ont des couleurs proches, l'équipe à l'extérieur prend sa couleur
  secondaire ; le texte posé sur une couleur est blanc ou noir selon sa luminance.
- **Logos en base** : la colonne `teams.logo_url` porte le lien de l'image sur le site de la NBA
  (`https://cdn.nba.com/logos/nba/{identifiant NBA}/global/L/logo.svg`). La migration Liquibase 015 la remplit pour
  les trente franchises actuelles, l'import des données la remplit pour les équipes créées ensuite ; un lien déjà
  saisi n'est jamais écrasé. Le frontend affiche seulement ce lien : aucune image n'est copiée dans le dépôt. Sans
  lien, ou si l'image ne charge pas, une pastille aux couleurs de l'équipe porte son abréviation.

## Conséquences

- **+** Un clic suffit pour prédire un match et voir si le modèle avait raison.
- **+** Les deux camps se lisent d'un coup d'œil sur la barre de probabilité, les facteurs et le classement.
- **−** Les logos sont des marques de la NBA et de ses franchises, affichés depuis leur site : acceptable pour une
  démonstration non commerciale, à remplacer par des pastilles seules pour un usage commercial.
- **−** Hors ligne, ou si la NBA change ses adresses, seules les pastilles s'affichent.
- **−** Les couleurs sont celles d'aujourd'hui, pas celles de l'époque pour les saisons anciennes.
