# 0010 · Calendrier par équipe, logos et couleurs des franchises

## Problème

Pour prédire un match, il fallait choisir deux équipes, une saison et un jour à la main, puis comparer soi-même avec
le vrai résultat. La page Matchs filtrait déjà par équipe, mais du plus récent au plus ancien, sans bilan ni lecture
du point de vue de l'équipe. Toutes les équipes avaient la même couleur : on distinguait mal les deux camps d'une
prédiction.

## Décision

- **Calendrier** (ancienne page Matchs, même URL `/games`) : une équipe choisie donne son calendrier de la saison,
  du premier au dernier match, par mois, avec l'adversaire, le lieu (« vs » ou « @ »), le résultat (V ou D et le
  score) et le prochain match signalé. Chaque ligne ouvre la prédiction de ce match. Sans équipe choisie, la liste
  de tous les matchs reste en place.
- **Prédire part du calendrier** : la page propose d'abord « Choisir un match » parmi ceux en base, soit par équipe
  (liste de ses matchs de la saison, le prochain proposé et prédit aussitôt), soit par jour (tous les matchs du
  jour). La saisie libre de deux équipes et d'un jour reste possible, repliée sous « Ou imaginer une autre affiche ».
- **Résultat réel** : le lien du calendrier porte l'identifiant du match (`game`). La page Prédire lit ce match et,
  s'il a été joué, affiche le score et si le favori du modèle a gagné. Changer une équipe, la saison ou le jour fait
  oublier ce match.
- **Couleurs** : les couleurs officielles des trente franchises sont dans le code (`team-branding.ts`), par
  abréviation. Quand les deux équipes d'un match ont des couleurs proches, l'équipe à l'extérieur prend sa couleur
  secondaire ; le texte posé sur une couleur est blanc ou noir selon sa luminance.
- **Logos** : le navigateur charge le logo depuis le site de la NBA (`cdn.nba.com`, par identifiant de franchise),
  sauf si l'équipe a un `logoUrl` en base. Aucun logo n'est copié dans le dépôt. Si l'image ne charge pas, une
  pastille aux couleurs de l'équipe porte son abréviation.

## Conséquences

- **+** Un clic suffit pour prédire un match et voir si le modèle avait raison.
- **+** Les deux camps se lisent d'un coup d'œil sur la barre de probabilité, les facteurs et le classement.
- **−** Les logos sont des marques de la NBA et de ses franchises, affichés depuis leur site : acceptable pour une
  démonstration non commerciale, à remplacer par des pastilles seules pour un usage commercial.
- **−** Hors ligne, ou si la NBA change ses adresses, seules les pastilles s'affichent.
- **−** Les couleurs sont celles d'aujourd'hui, pas celles de l'époque pour les saisons anciennes.
