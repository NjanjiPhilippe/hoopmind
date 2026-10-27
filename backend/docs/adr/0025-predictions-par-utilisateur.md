# 0025 · Chaque utilisateur garde ses prédictions, et ne voit que les siennes

## Problème

Une prédiction demandée sur la page Prédire disparaissait dès qu'on quittait la page : impossible de revoir ses
pronostics ni de savoir s'ils étaient bons. L'ancienne table `match_predictions` (retirée par l'ADR 0024) n'était
remplie par personne, et ses routes de lecture montraient les pronostics de tout le monde à n'importe quel compte.

## Décision

- Chaque appel à `GET /api/v1/spark/ml/predict/game/explanation` enregistre la prédiction pour l'utilisateur du jeton
  (table `prediction_history`, migration 018) : saison, équipes, date, playoffs, probabilité, score prédit, modèle.
  Redemander le même match remplace la ligne (le modèle ou la forme des équipes ont pu changer).
- `GET /api/v1/predictions/mine` (paginé, plus récentes d'abord) et `DELETE /api/v1/predictions/mine/{id}` lisent
  l'identifiant **uniquement dans le jeton**, jamais dans la requête. Supprimer la prédiction d'un autre répond 404,
  comme une prédiction qui n'existe pas : on ne révèle pas qu'elle existe.
- Le score réel et le verdict (bon ou raté) sont calculés à la lecture, à partir des matchs joués (`GameHistory`) :
  un score saisi ou importé plus tard apparaît sans rien recalculer.
- La table ne référence les équipes et la saison que par identifiant côté Java (le module ML reste fermé, ADR 0024),
  avec des clés étrangères en base : supprimer un compte supprime ses prédictions.
- Frontend : page « Mes prédictions » (menu de tous les comptes) et bloc « Mes dernières prédictions » sur l'accueil ;
  l'accueil d'un administrateur ajoute ses raccourcis de gestion. Les pages d'administration restaient déjà cachées
  (menu) et protégées des deux côtés (garde Angular, `/api/v1/admin/**` et `@PreAuthorize` côté serveur).

## Conséquences

- Un échec d'écriture dans l'historique (deux demandes simultanées du même match) est journalisé, la prédiction est
  quand même renvoyée.
- `/predict/game` (sans explication) n'enregistre rien : seul l'écran utilisé par les utilisateurs alimente l'historique.

## Alternatives écartées

- **Filtrer côté frontend** : n'importe quel compte pourrait toujours lire les prédictions des autres par l'API.
- **Stocker le verdict** : il faudrait le recalculer à chaque score saisi, importé ou corrigé.
