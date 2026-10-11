# 0006 · Les imports et l'entraînement sont tracés dans l'audit

## Problème

Le journal d'audit de la PR #1 couvrait les comptes et les sessions, mais pas les actions les plus lourdes d'un
administrateur : remplacer les données NBA (Walsh, CSV) ou réentraîner le modèle qui sert les prédictions. Si les
prédictions changent du jour au lendemain, on ne pouvait pas savoir qui avait lancé quoi, ni quand.

## Décision

- `ingestion` publie `DataImportEvent` (scellée) : `NbaDatabaseImported`, `NbaDatabaseImportFailed`,
  `ReferenceCsvImported`.
- `ml` publie `ModelTrainingEvent` (scellée) : `ModelTrained`, `ModelTrainingFailed`.
- L'audit les enregistre sous deux nouvelles actions, `DATA_IMPORTED` et `MODEL_TRAINED`, avec réussite ou échec.
  La cible est le jeu de données (`DATASET`) ou le modèle (`MODEL`, avec sa version), et les détails sont courts :
  saisons, nombre de matchs, fichiers importés ou en échec, exactitude du modèle, message d'erreur tronqué.
- **L'auteur est capturé au lancement** et transporté dans l'événement (`requestedBy`). L'import se termine dans un
  fil d'arrière-plan où il n'y a plus d'utilisateur connecté. Pour l'import au démarrage, l'auteur est « système ».

## Alternatives écartées

- **Tracer le début et la fin** : deux lignes par opération. La fin suffit pour savoir qui a changé les données ;
  les jobs persistés (étape suivante) donneront l'historique complet et l'état « interrompu ».
- **Résoudre l'auteur dans l'écouteur, comme pour les comptes** : impossible pour un job, car l'écouteur ne voit
  plus la requête d'origine. C'est la limite annoncée dans l'ADR 0004.

## Avantages

- On sait qui a remplacé les données ou le modèle, quand, et avec quel résultat.
- Les échecs sont tracés aussi, ce qui aide au diagnostic sans aller lire les journaux du serveur.
- Même mécanisme que pour les comptes : `ingestion` et `ml` ne dépendent pas de l'audit.

## Inconvénients

- `ingestion` et `ml` dépendent de `auth` (pour `CurrentUser`). C'est une dépendance vers une API publique
  et stable, sans cycle.
- Deux nouvelles valeurs d'`AuditAction` : la page Journal du front devra leur donner un libellé (étape frontend).
