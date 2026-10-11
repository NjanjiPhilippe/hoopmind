# 0002 · Après trop de tentatives de connexion, un décompte bloque le bouton

## Problème

Le backend limite les connexions (ADR 0007 du backend) et répond 429 avec le délai d'attente, dans le corps
(`retryAfterSeconds`) et dans l'en-tête `Retry-After`. La page de connexion affichait seulement « erreur », et
l'utilisateur pouvait recliquer, ce qui prolongeait le blocage. Les champs de mot de passe ne pouvaient pas être
affichés, et le changement de mot de passe n'avait pas de champ de confirmation.

## Décision

- `retryAfterSeconds(error)` lit le délai dans le corps, puis dans l'en-tête à défaut.
- La page affiche « réessayez dans 29 s », le bouton devient « Réessayer dans 29 s » et reste désactivé jusqu'à la
  fin du décompte.
- Un composant partagé `app-password-input` ajoute un bouton Afficher/Masquer. Il est utilisé dans la connexion,
  l'inscription et la page Compte.
- Les règles du mot de passe sont visibles dès l'arrivée sur le formulaire, plus seulement après une erreur.
- Le changement de mot de passe demande une confirmation, vérifiée par un validateur de groupe.

## Alternatives écartées

- **Un message générique sans délai** : l'utilisateur ne sait pas combien de temps attendre et réessaie trop tôt.
- **Laisser le bouton actif** : chaque clic renvoie un 429 et peut rallonger le blocage.

## Avantages

- L'utilisateur légitime sait exactement quand réessayer, et la limite reste aussi stricte pour un attaquant.
- Moins d'erreurs de frappe dans les mots de passe, sans affaiblir la sécurité.

## Inconvénients

- Le décompte vit dans l'onglet : un rechargement l'efface, et le prochain essai renvoie un nouveau 429 avec le
  délai restant. C'est le serveur qui fait foi.
