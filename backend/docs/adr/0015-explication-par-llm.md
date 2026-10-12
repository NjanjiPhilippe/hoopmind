# 0015 · Explication des prédictions par un LLM, avec nos propres adaptateurs plutôt que Spring AI

## Problème

Le modèle donne une probabilité (« Boston 71 % ») mais ne dit pas pourquoi. Pour un fan, le chiffre seul ne
suffit pas ; pour le projet, c'est l'occasion de brancher un LLM sur un vrai besoin. Trois contraintes :

1. **Gratuit.** Le projet doit tourner sans abonnement payant : l'offre gratuite de Google Gemini, de Groq, ou un
   modèle local avec Ollama doivent suffire. Claude (payant) doit rester possible.
2. **Exact.** Un LLM qui « explique » en inventant une blessure ou une statistique est pire que pas d'explication.
3. **Toujours disponible.** Les offres gratuites ont des quotas (HTTP 429) et les services tombent : la page ne
   doit jamais rester sans réponse à cause du LLM.

## Décision

### Le modèle calcule, le LLM rédige

- La régression logistique est décomposable exactement : la marge (log-cote) vaut l'ordonnée à l'origine plus,
  pour chaque variable, `poids × valeur standardisée`. `ScoringCard.contributions` calcule ces parts, et
  `homeWinProbability` s'en sert : la probabilité et son explication viennent du même calcul.
- `GameOutcomePredictor.analyze` renvoie la prédiction **et** ses facteurs (`PredictionFactor` : valeur de chaque
  équipe, contribution). Le LLM ne reçoit que ces chiffres, triés du plus au moins influent, et les met en mots.
- Le prompt (`ExplanationPrompt`, identique pour tous les fournisseurs) interdit d'inventer, de contredire la
  probabilité ou de donner un conseil de pari. Les données sont dans une balise `<donnees>` présentée comme des
  données, pas des consignes, pour qu'un nom d'équipe ne puisse pas se faire passer pour une instruction.

### Architecture hexagonale, comme dans ledger-ai

- Port `PredictionExplainer` : `Explanation explain(ExplanationRequest)`. Le reste du module ne sait pas qu'un LLM
  existe ; le service `PredictionExplanationService` dépend du port seulement.
- Deux adaptateurs, choisis par `hoopmind.ml.explanation.provider` (`LLM_PROVIDER`) :
  - `openai-compatible` : `POST /chat/completions`. **Un seul code pour Gemini, Groq, Ollama et OpenAI**, qui
    exposent tous ce format ; on change l'adresse, la clé et le modèle.
  - `anthropic` : `POST /v1/messages` (en-têtes `x-api-key` et `anthropic-version`). Seuls les blocs `text` sont
    gardés ; un `stop_reason` « refusal » est un refus.
- `none` (par défaut) : pas d'adaptateur, l'explication est rédigée sans LLM. L'application fonctionne sans clé.

### Résilience et quotas

- **Erreurs en deux familles** (`LlmException`, scellée) : `LlmUnavailableException` (réseau, délai dépassé,
  408, 429, 5xx) est retentée ; `LlmRejectedException` (clé refusée, modèle inconnu, réponse vide ou refusée) ne
  l'est pas, la retenter ne changerait rien.
- `@Retryable` de Spring 7 : 2 nouvelles tentatives au plus, délai exponentiel (1 s, 2 s) avec *jitter*.
- `@ConcurrencyLimit` : 4 appels simultanés au plus vers le LLM, les suivants attendent. C'est le cas d'usage
  que l'ADR 0011 réservait à cette annotation : un appel coûteux, synchrone, sur un service à quota.
- Délai maximal de 20 s par appel (`LLM_TIMEOUT`).
- **Cache en mémoire** (500 entrées, les moins récemment lues partent en premier) : une explication par match,
  date, saison et version du modèle. Un nouvel entraînement change la version, donc les explications. Seules les
  réponses du LLM sont gardées : après un échec, la question suivante retente le LLM.
- **Repli** : si le LLM échoue, `TemplateExplanation` rédige une explication à partir des mêmes chiffres
  (`fallback: true` dans la réponse). Plus sèche, mais exacte et immédiate.

### Endpoint

`GET /api/v1/spark/ml/predict/game/explanation` (mêmes paramètres que `/predict/game`, utilisateur connecté)
renvoie les équipes, la prédiction, les facteurs triés, l'explication (texte, fournisseur, modèle) et les drapeaux
`cached` et `fallback`.

## Pourquoi pas Spring AI ?

Spring AI aurait fonctionné : il sait parler à OpenAI, Gemini, Ollama et Anthropic. Mais il ne remplace pas
l'architecture, il se logerait **dans** l'adaptateur. La vraie question était donc : que met-on dans l'adaptateur ?

- **Le besoin est petit.** Un appel, sans streaming, sans outils, sans RAG, sans mémoire de conversation : texte
  en entrée, texte en sortie. Deux adaptateurs d'une centaine de lignes chacun couvrent cinq fournisseurs.
- **Moins de dépendances à aligner.** Le projet suit Spring Boot 4.1, Spring Framework 7 et Jackson 3, et a déjà dû
  régler un conflit de versions entre Spark et Hibernate. Une dépendance de plus, c'est une version de plus à garder
  compatible à chaque montée de version de Spring Boot.
- **Tout est visible et réglable.** Le prompt exact, les en-têtes, le délai, et surtout la politique d'erreurs
  (retenter un 429, pas un 401) sont dans notre code, avec les outils déjà en place (`RestClient`, `@Retryable`,
  `@ConcurrencyLimit`).
- **Testable sans le framework.** Les tests démarrent un faux fournisseur HTTP et vérifient la requête réellement
  envoyée (chemin, en-têtes, corps) et la réponse lue, y compris les blocs de raisonnement ignorés et les refus.
- **Même approche que ledger-ai**, où les adaptateurs OpenAI et Anthropic sont aussi écrits à la main derrière un
  port : un seul style d'architecture d'un projet à l'autre.

Les SDK officiels (OpenAI, Anthropic) ont été écartés pour la même raison : une dépendance par fournisseur, pour un
seul appel.

## Avantages

- Gratuit par défaut (Gemini, Groq, Ollama), Claude en changeant trois variables d'environnement.
- Le LLM ne peut pas fausser la prédiction : il n'a que les chiffres du modèle, et la probabilité ne vient pas de lui.
- L'explication est toujours là : cache, nouvelles tentatives, puis repli sans LLM.
- Aucune dépendance ajoutée.

## Inconvénients

- Les formats d'API sont maintenus par nous : si un fournisseur change le sien, c'est à nous d'adapter le code.
- Pas de streaming, pas de sortie structurée, pas d'appel d'outils. **Si un assistant conversationnel avec outils
  (« qui gagne Celtics–Lakers, et pourquoi ? ») est ajouté, Spring AI ou les SDK officiels deviendront
  intéressants** : boucle d'outils, mémoire, streaming. La décision sera revue à ce moment-là.
- Le cache est en mémoire : il est perdu au redémarrage et n'est pas partagé entre plusieurs instances. Suffisant
  pour une instance unique.
- Le repli sans LLM est moins naturel qu'un texte généré.
