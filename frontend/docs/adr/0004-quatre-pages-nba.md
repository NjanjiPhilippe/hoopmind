# 0004 · Les pages NBA héritées sont remplacées par quatre pages branchées sur l'API

## Problème

L'espace connecté contenait cinq pages héritées (équipes, joueurs, matchs, prédictions, statistiques) et leurs
services. Elles appelaient des URL que le backend ne sert pas, ou affichaient des données inventées. L'utilisateur
arrivait sur des écrans vides ou faux, et le seul parcours qui a de la valeur, prédire un match, n'existait pas.

## Décision

Quatre pages, chacune construite sur un endpoint réel :

- **Prédire** : deux équipes, une saison, un jour, l'option playoffs. Les choix sont dans l'URL, donc un lien
  ouvre la prédiction déjà calculée. Le résultat montre une barre de probabilité et rappelle la précision du
  modèle face à la règle « le meilleur bilan gagne ». Un modèle non entraîné donne un avertissement, avec un lien
  vers la page Données pour un administrateur.
- **Classement** : les bilans de la saison, par conférence.
- **Matchs** (devenue **Calendrier**, voir [ADR 0010](0010-calendrier-logos-couleurs.md)) : filtres par équipe et par type, pagination par 50, un lien « Prédire » sur chaque match.
- **Accueil connecté** : derniers résultats, prochains matchs, état du modèle, et un état vide qui explique quoi
  faire quand aucune donnée n'a été importée.

La page d'accueil publique est réécrite sans appel à l'API ni promesse invérifiable. Les cinq pages, les anciens
services, modèles et composants (`match-card`, `team-card`, etc.) sont supprimés. Un `NbaCatalog` fourni par page
partage les saisons et les équipes.

## Alternatives écartées

- **Réparer les cinq pages** : la moitié dépend de données que le backend n'a pas (joueurs, statistiques
  détaillées) ; ce serait du travail pour des écrans qui resteraient vides.
- **Un `NbaCatalog` global (`providedIn: 'root'`)** : il garderait des équipes périmées après un import ; fourni
  par page, il se recharge à chaque visite.

## Avantages

- Chaque écran affiche des données vraies, et le parcours principal (voir un match, le prédire) tient en deux clics.
- Moins de code : les pages et services supprimés ne sont plus à maintenir.

## Inconvénients

- Plus de page joueurs ni de statistiques détaillées. Elles reviendront quand le backend servira ces données.
- Les saisons et les équipes sont rechargées à chaque page (deux petites requêtes).
