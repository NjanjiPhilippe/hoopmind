# 0014 · Import par lots réécrits, une seule transaction, horodatages et rôles

## Import plus rapide : lots réécrits par le pilote

**Problème.** L'import envoyait des dizaines de milliers d'`INSERT … ON CONFLICT` en `batchUpdate`. Sans option,
le pilote PostgreSQL exécute quand même chaque ligne comme une instruction séparée, avec un aller-retour par ligne.

**Décision.**
- `reWriteBatchedInserts=true` (propriété Hikari dans `application.yml`). Le pilote réécrit chaque lot en un seul
  `INSERT … VALUES (…), (…), …`. C'est l'optimisation recommandée par la documentation de pgJDBC pour les
  insertions en masse.
- Lots de 1 000 lignes, pour borner la mémoire et la taille des requêtes.
- Effet de bord traité : avec la réécriture, le pilote ne renvoie plus le nombre de lignes touchées par chaque
  instruction. L'import CSV, qui s'en servait pour compter les lignes nouvelles et modifiées, les compte maintenant
  par leur date d'import (`imported_at`, que l'upsert ne touche que si la ligne change). Le test qui vérifiait ces
  comptes l'a détecté.

**Limite.** Le gain n'a pas été mesuré ici : la base de démonstration est trop petite pour que ce soit significatif.

## Une seule transaction pour l'import (décision inverse de la revue)

La revue proposait une transaction **par saison**. Après les autres changements, je propose de **garder une seule
transaction** :
- un import est **tout ou rien** : jamais de base à moitié importée, avec une saison 2023 nouvelle et une 2024 ancienne ;
- le risque d'une longue transaction, qui bloque les autres écritures, est traité autrement : le verrou des
  données (ADR 0005) empêche toute autre écriture concurrente, et PostgreSQL (MVCC) ne bloque pas les lectures ;
- le découpage par saison compliquerait le code (équipes et saisons sont partagées entre les lots).

Ce qu'on perd : en cas d'échec à la 9ᵉ saison, les 8 premières sont aussi annulées. L'import étant rejouable, il suffit
de le relancer.

## Cohérence

- **Saisons réellement importées** : le compte rendu listait les saisons *demandées*, y compris celles écartées
  faute de match de saison régulière. Il liste maintenant celles qui ont été écrites, et les matchs écartés
  s'ajoutent à « ignorés ».
- **Horodatages** : `reference_csv_rows.imported_at` passe en `TIMESTAMP WITH TIME ZONE` (Liquibase 013), comme le
  reste du schéma récent. Les valeurs existantes sont considérées comme écrites en UTC (heure du conteneur).
  L'API renvoie un `Instant`. Les dates « maintenant » passent par l'horloge injectée (`Clock`), donc sont testables.
- **Expression régulière précompilée** : `String.matches` recompilait le motif des nombres pour **chaque case**
  de chaque CSV. C'est maintenant un `Pattern` statique.
- **Rôles** : enregistrer les mêmes rôles produisait une entrée d'audit « rôles modifiés » et déconnectait
  l'utilisateur. Rien ne se passe désormais si rien ne change (test ajouté).
- **Configuration** : `MlProperties` (`@ConfigurationProperties`, valeurs par défaut typées) remplace les `@Value`
  du module ML.
