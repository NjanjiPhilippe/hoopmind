# 0019 · Monitoring : Micrometer, Prometheus et Grafana, sur trois niveaux

## Problème

L'application tourne, mais rien ne dit comment : combien de requêtes, à quelle vitesse, combien d'erreurs. Deux
parties sont propres au projet et méritent leur propre suivi :

1. **Le LLM** est un service externe gratuit, donc lent par moments, limité en quota et parfois indisponible.
   Il faut voir combien d'explications viennent vraiment du LLM, combien sont des textes de secours, et ce
   que coûtent les appels en tokens.
2. **Le modèle de prédiction** peut se dégrader sans aucune erreur : si les données changent, les probabilités
   se déplacent. La version publique n'enregistre pas les prédictions, donc on ne peut pas suivre la précision
   au fil des matchs ; on peut en revanche suivre la répartition des probabilités et la version servie.

## Décision

### Métriques

Micrometer (déjà fourni par Spring Boot Actuator) avec le registre Prometheus. Toutes les métriques portent le
tag `application=hoopmind`.

| Niveau      | Métrique                                    | Tags                              |
|-------------|---------------------------------------------|-----------------------------------|
| Application | `http.server.requests` (histogramme)        | `uri`, `status`, `method`         |
| Application | JVM, pool de connexions, journaux (Boot)    |                                   |
| LLM         | `hoopmind.explanations` (compteur)          | `source` : `llm`, `cache`, `template`, `fallback` |
| LLM         | `hoopmind.llm.calls` (durée, histogramme)   | `provider`, `outcome` : `success`, `unavailable`, `rejected` |
| LLM         | `hoopmind.llm.tokens` (compteur)            | `provider`, `model`, `direction` : `input`, `output` |
| Modèle      | `hoopmind.ml.predictions` (durée, histogramme) | `model_version`, `outcome` : `ok` ou le code d'erreur |
| Modèle      | `hoopmind.ml.home.win.probability` (répartition, tranches de 0,1) | `model_version` |

- La durée d'un appel au LLM comprend les nouvelles tentatives de `@Retryable` : c'est ce que l'utilisateur attend.
- Les tokens viennent de la réponse du fournisseur (`usage`). Ils ne sont pas renvoyés au frontend
  (`@JsonIgnore` sur `Explanation`).
- Pas de tag par équipe ou par match : le nombre de séries resterait borné par le nombre de versions du modèle.

### Exposition protégée

`/actuator/prometheus` a sa propre chaîne de sécurité : HTTP Basic avec un compte de collecte
(`hoopmind.monitoring.scrape`, mot de passe `METRICS_PASSWORD`), distinct des comptes HoopMind, comparé en temps
constant. Sans mot de passe configuré, l'endpoint reste fermé. Les métriques donnent les routes et les volumes :
ce n'est pas une information publique.

### Prometheus et Grafana dans le docker-compose

Profil `monitoring` : Prometheus lit l'API toutes les 15 secondes avec le mot de passe monté comme secret
Docker, et Grafana affiche le tableau de bord versionné `monitoring/grafana/dashboards/hoopmind.json`, chargé au
démarrage (source de données et tableau de bord provisionnés, pas de clic manuel). Les deux ports ne sont
ouverts que sur la machine locale.

## Alternatives écartées

- **Endpoint ouvert sur un port de gestion séparé.** Plus simple pour Prometheus, mais il faut alors être sûr que
  ce port n'est jamais publié, et la vérification de santé du conteneur change d'adresse.
- **OpenTelemetry et une plateforme hébergée (Grafana Cloud, Datadog).** Plus complet (traces), mais demande un
  compte externe pour lancer le projet. Micrometer peut exporter en OTLP plus tard sans changer les métriques.
- **Langfuse ou un outil dédié aux LLM.** Utile pour relire les prompts et les réponses ; ici le prompt est fixe
  et les métriques suffisent.

## Conséquences

- `docker compose --profile app --profile monitoring up -d` donne un tableau de bord prêt sur `localhost:3000`.
- Ajouter une métrique, c'est l'enregistrer dans le code et ajouter un panneau au JSON du tableau de bord.
- Le suivi de la précision du modèle dans le temps demande d'enregistrer les prédictions, ce que la version
  publique ne fait pas.
