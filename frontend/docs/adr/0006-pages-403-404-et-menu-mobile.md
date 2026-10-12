# 0006 · Pages 403 et 404, menu mobile accessible

## Problème

Une URL inconnue ou une page admin ouverte par un simple utilisateur renvoyait silencieusement vers l'accueil :
l'utilisateur ne savait pas pourquoi. Sur mobile, la barre de navigation débordait, et le menu ne se fermait ni
après un clic ni avec Échap.

## Décision

- Une page d'erreur unique affiche 403 ou 404 selon la route. Le garde admin envoie vers `/forbidden`, la route
  `**` vers la page 404. Un utilisateur déjà connecté qui ouvre la connexion va sur `/dashboard`.
- Le menu mobile a un bouton avec `aria-expanded` et `aria-controls` ; il se ferme à chaque navigation et sur Échap.
- Chaque route a un titre (« Prédire un match · HoopMind »), lu par les lecteurs d'écran et visible dans l'onglet.

## Alternatives écartées

- **Rediriger vers l'accueil avec un message flash** : l'URL fautive disparaît, ce qui complique le partage d'un
  lien cassé.

## Avantages

- L'utilisateur comprend ce qui se passe, et le parcours mobile est utilisable au clavier.

## Inconvénients

- Une 403 confirme qu'une page existe ; elle reste protégée côté serveur, la page ne révèle rien d'autre.
