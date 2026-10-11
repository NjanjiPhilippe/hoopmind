# 0011 · `@Retryable` sur le téléchargement Kaggle, pas de `@ConcurrencyLimit`

## Problème

Le téléchargement Kaggle (plusieurs centaines de Mo) échouait définitivement à la moindre coupure réseau, au moindre
HTTP 429 (limite de débit) ou à la moindre erreur 5xx passagère. L'administrateur devait alors tout relancer à la main.

## Décision

- Spring Framework 7 fournit la résilience **sans dépendance** (`@EnableResilientMethods` dans `shared.config`).
- `KaggleDownloader.download` est annoté `@Retryable` : 3 nouvelles tentatives au plus, avec un délai exponentiel
  (2 s, 4 s, 8 s, plafonné à 30 s) et du *jitter* pour ne pas frapper Kaggle en rythme.
- **On ne retente que ce qui peut se résoudre tout seul.** Une nouvelle exception `KaggleUnavailableException`
  couvre l'erreur réseau, le 429 et le 5xx. Une clé refusée (401/403) ou un fichier introuvable (404) échoue tout
  de suite, avec un message qui dit quoi corriger.
- Les délais se règlent par propriété (`hoopmind.import.kaggle.retry.*`), ce qui permet un test rapide.
- Un test démarre un mini contexte Spring (le vrai proxy `@Retryable`) face à un faux Kaggle : deux 503 puis un
  succès donnent 3 appels. Un 401 donne 1 seul appel.

## Pourquoi pas `@ConcurrencyLimit` ?

C'était prévu. À l'examen, il ne convient pas ici :

- il limite les appels **simultanés d'une méthode**, alors que notre contrainte est « une seule opération lourde sur
  les données, quelle qu'elle soit » (import, CSV ou entraînement) ;
- il se libère à la fin de l'appel de méthode, alors qu'un job vit au-delà de la requête qui l'a lancé ;
- sa politique `REJECT` lève une exception générique, alors que le verrou de l'ADR 0005 dit à l'administrateur
  *quelle* opération bloque.

Il resterait utile pour protéger un appel coûteux et synchrone, par exemple un futur endpoint d'analyse Spark à la
demande. Ce cas n'existe pas encore.

## Alternatives écartées

- **Resilience4j ou Spring Retry** : plus riches (disjoncteur, métriques), mais ce sont des dépendances de plus
  pour un seul appel. Le support natif de Spring 7 suffit.
- **Reprendre le téléchargement là où il s'est arrêté (HTTP Range)** : moins de données retéléchargées, mais
  Kaggle redirige vers un stockage dont la prise en charge des requêtes partielles n'est pas garantie.

## Avantages

- Les coupures passagères ne demandent plus d'intervention.
- Aucune dépendance ajoutée, et un comportement testé à travers le vrai proxy Spring.

## Inconvénients

- Une nouvelle tentative recommence le téléchargement depuis le début, et la barre de progression repart de zéro.
- Un disque plein est aussi retenté 3 fois, puisqu'on ne distingue pas l'erreur d'écriture de l'erreur de lecture
  réseau. Le coût est de quelques secondes avant le même échec.
