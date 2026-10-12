# 0007 · Les gabarits lisent les ressources avec `currentValue()`

## Problème

Depuis Angular 20, `resource.value()` **lève une exception** quand la ressource est en erreur. Une requête annulée
par une navigation, ou un 500, faisait planter le rendu du composant entier au lieu d'afficher un message.

## Décision

Un utilitaire `currentValue(resource)` renvoie la valeur si la ressource en a une, sinon `undefined`. Les gabarits
et les `computed` l'utilisent ; l'état d'erreur est affiché à part avec `resource.error()`.

## Alternatives écartées

- **`@if (r.hasValue())` partout** : correct, mais verbeux et facile à oublier dans un `computed`.
- **Le nom `valueOf`** : entre en collision avec `Object.prototype.valueOf`.

## Avantages

- Une erreur réseau n'affecte plus que la carte concernée.
- Une règle simple à relire en revue : jamais `.value()` sur une ressource dans un gabarit.

## Inconvénients

- `undefined` ne distingue pas « en chargement » de « en erreur » ; les gabarits lisent `isLoading()` et `error()`
  pour faire la différence.
