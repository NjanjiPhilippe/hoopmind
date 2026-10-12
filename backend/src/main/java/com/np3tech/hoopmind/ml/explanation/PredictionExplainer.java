package com.np3tech.hoopmind.ml.explanation;

/**
 * Port d'explication d'une prédiction en langage naturel. Le module ML ne sait pas quel LLM répond : l'adaptateur
 * actif (API compatible OpenAI pour Gemini, Groq, Ollama ou OpenAI ; API Anthropic pour Claude) se choisit par
 * configuration ({@code hoopmind.ml.explanation.provider}). Sans adaptateur, une explication rédigée par
 * {@link TemplateExplanation} prend le relais.
 */
public interface PredictionExplainer {

    /**
     * @throws LlmUnavailableException si le service est momentanément indisponible (réseau, délai dépassé,
     *                                 quota 429, erreur 5xx) : l'appel peut être retenté
     * @throws LlmRejectedException    si la demande est refusée (clé invalide, modèle inconnu, réponse vide) :
     *                                 la retenter ne changerait rien
     */
    Explanation explain(ExplanationRequest request);
}
