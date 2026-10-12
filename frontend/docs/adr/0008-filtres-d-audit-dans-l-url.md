# 0008 · Les filtres du journal d'audit vivent dans l'URL

## Problème

Les filtres du journal d'audit (action, auteur, dates, page) étaient un état local : un rechargement les perdait,
et on ne pouvait pas envoyer à un collègue le lien d'une recherche. Sur mobile, le tableau débordait. Les détails
des événements s'affichaient en JSON brut.

## Décision

- Les filtres sont lus depuis les paramètres de requête (liés aux inputs du composant par
  `withComponentInputBinding`). Changer un filtre, une page ou tout réinitialiser passe par `router.navigate`.
- Sous 640 px, chaque événement devient une carte.
- Les détails sont décrits en français (« 168 matchs », « Précision : 58,3 % »). Un événement sans auteur
  est attribué au « Système ».

## Alternatives écartées

- **Un état dans un service** : survit à la navigation, pas au rechargement ni au partage.

## Avantages

- Le bouton Précédent du navigateur annule le dernier filtre, et un lien reproduit exactement une recherche.

## Inconvénients

- Chaque changement de filtre ajoute une entrée dans l'historique du navigateur.
