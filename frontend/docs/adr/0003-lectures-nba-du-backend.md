# 0003 · Les lectures NBA du backend chargent leurs relations et renvoient les scores

## Problème

En branchant les nouvelles pages sur l'API, les lectures de matchs et du classement répondaient **500**. Depuis
`open-in-view: false`, la réponse est construite après la fermeture de la session Hibernate ; lire `homeTeam` ou
`season` d'un match levait alors une `LazyInitializationException`. Les matchs ne portaient pas non plus leur
score, qui est stocké dans une table de résultats séparée.

## Décision

- `@EntityGraph` sur les méthodes de `GameRepository` (équipes et saison), de `TeamStatsRepository` (équipe et
  saison) et sur `GameResultRepository.findByGameIdIn` (match et vainqueur).
- `GameService.getResults(games)` charge les résultats d'une liste de matchs en **une** requête, et `GameMapper`
  ajoute le score à chaque match joué. Un match programmé n'a pas de champ `result`.
- `NbaReadApiTests` vérifie, contre PostgreSQL, les matchs par saison avec scores, un match programmé, un match par
  identifiant, les matchs d'une équipe et le classement.

## Alternatives écartées

- **Réactiver `open-in-view`** : masque le problème, garde une connexion ouverte pendant toute la sérialisation et
  génère des requêtes N+1.
- **`FetchType.EAGER` sur les entités** : charge les relations partout, même là où elles ne servent pas.
- **Une requête de résultat par match** : N+1 requêtes sur une page de 50 matchs.

## Avantages

- Les pages publiques fonctionnent et le nombre de requêtes SQL reste constant par page.
- Le contrat est figé par un test d'intégration.

## Inconvénients

- Chaque nouvelle méthode de dépôt devra déclarer son graphe ; le test d'intégration rattrape l'oubli sur les
  endpoints couverts.
