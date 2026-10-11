# 0005 · Le tableau de bord admin n'affiche que des chiffres réels

## Problème

Le tableau de bord admin affichait des compteurs codés en dur et des boutons qui ne menaient nulle part.

## Décision

Il lit le nombre de comptes (`totalElements` de la liste des utilisateurs), l'état de la base NBA, le modèle en
service et les cinq derniers événements d'audit. Chaque carte mène à la page qui permet d'agir. Les boutons sans
destination sont retirés.

## Alternatives écartées

- **Un endpoint backend dédié aux statistiques** : une requête de moins, mais un endpoint de plus à maintenir
  pour une page peu visitée.

## Avantages

- L'administrateur voit d'un coup d'œil si les données sont importées et si le modèle est fiable.

## Inconvénients

- Quatre requêtes à l'ouverture de la page ; chaque carte gère son propre état de chargement et d'erreur.
