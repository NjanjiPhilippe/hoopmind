# 0028 · Plafond quotidien du LLM, quota sans nouvel essai, prédictions bornées

## Problème

Avec une démonstration publique, le quota gratuit du fournisseur de LLM devient une ressource partagée :

- le quota par utilisateur et par heure (ADR 0015) ne bornait pas le total : plusieurs comptes suffisaient à vider le
  quota du jour pour tous ;
- un 429 (quota atteint) était retenté deux fois : chaque essai consommait encore le quota, et trois appels de 20 s
  plus les pauses dépassaient le délai de nginx (60 s), qui répondait 504 au lieu de l'explication sans LLM ;
- chaque date demandée créait une nouvelle explication, et aucune date n'était refusée, même en 2099 ;
- deux demandes simultanées de la même explication faisaient deux appels ;
- chaque prédiction expliquée ajoutait une ligne à « Mes prédictions », sans limite.

Une prédiction pouvait aussi opposer une équipe à elle-même : seul le frontend l'empêchait.

## Décision

- **Plafond quotidien commun** (`LLM_MAX_CALLS_PER_DAY`, 200 par défaut, jour UTC). Au-delà, tout le monde reçoit
  l'explication rédigée sans LLM jusqu'au lendemain.
- **Un 429 n'est pas retenté** (`LlmQuotaExceededException`) : l'explication sans LLM répond tout de suite. Les erreurs
  passagères (réseau, délai, 5xx) restent retentées.
- **Délai d'un appel : 10 s par défaut** (`LLM_TIMEOUT`) : trois essais et leurs pauses restent sous les 60 s de nginx.
- **Une seule requête au LLM pour une même explication** demandée en même temps : les suivantes attendent sa réponse.
- **Date bornée** : du 1er juillet de l'année de début de saison au 31 octobre suivant (la saison 2019-20 s'est
  finie en octobre 2020). Hors de cette fenêtre : 422 `DATE_OUTSIDE_SEASON`. La date reste dans la clé du cache, car
  le prompt la cite.
- **Équipe contre elle-même** : 400 `INVALID_MATCHUP`, vérifié par le serveur.
- **200 prédictions gardées par utilisateur** : au-delà, les plus anciennes sont supprimées à l'enregistrement.

## Alternatives écartées

- **Retirer la date de la clé du cache** : le prompt cite la date du match, l'explication en dépend.
- **Enregistrer les prédictions par un POST explicite** : plus juste pour une requête qui écrit, mais la page Prédire,
  le client généré et « Mes prédictions » changeraient ensemble. Le plafond suffit à borner l'effet du GET.
- **Un plafond en base, partagé entre instances** : HoopMind tourne sur une seule instance.

## Inconvénients assumés

- Le plafond quotidien est en mémoire : un redémarrage le remet à zéro.
- Un quota par minute dépassé de peu (offre gratuite) donne l'explication sans LLM, alors qu'un nouvel essai une
  seconde plus tard aurait pu réussir.
