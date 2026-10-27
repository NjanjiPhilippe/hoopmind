package com.np3tech.hoopmind.ml.explanation;

import com.np3tech.hoopmind.ml.GameOutcomePrediction;
import com.np3tech.hoopmind.ml.PredictionFactor;
import com.np3tech.hoopmind.ml.ScorePrediction;

import java.util.List;

/**
 * Explication rédigée sans LLM, à partir des mêmes chiffres : quand aucun fournisseur n'est configuré, ou quand
 * le LLM ne répond pas. Plus sèche qu'un texte généré, mais toujours exacte et toujours disponible.
 */
public final class TemplateExplanation {

    public static final String PROVIDER = "template";

    /** En dessous, un facteur ne pèse presque rien et n'est pas mentionné. */
    private static final double NEGLIGIBLE = 0.05;

    private TemplateExplanation() {
    }

    public static Explanation of(ExplanationRequest request) {
        GameOutcomePrediction prediction = request.analysis().prediction();
        boolean homeFavorite = prediction.homeTeamWins();
        String favorite = homeFavorite ? request.homeTeam() : request.awayTeam();
        double probability = homeFavorite ? prediction.homeWinProbability() : prediction.awayWinProbability();

        StringBuilder text = new StringBuilder("Le modèle voit ").append(favorite).append(" gagner, avec ")
                .append(ExplanationFacts.percent(probability)).append(" de chances.");
        if (probability < 0.55) {
            text.append(" Le match reste très ouvert.");
        }

        List<PredictionFactor> factors = ExplanationFacts.byInfluence(request.analysis().factors()).stream()
                .filter(factor -> !factor.feature().equals("is_playoff") || request.playoff())
                .filter(factor -> Math.abs(factor.contribution()) >= NEGLIGIBLE)
                .filter(factor -> !ExplanationFacts.counterIntuitive(factor))
                .limit(3)
                .toList();
        boolean first = true;
        for (PredictionFactor factor : factors) {
            boolean favorsHome = factor.contribution() >= 0;
            String beneficiary = favorsHome ? request.homeTeam() : request.awayTeam();
            boolean againstFavorite = favorsHome != homeFavorite;
            text.append(' ').append(first ? "Le facteur le plus important" : againstFavorite ? "En revanche, le facteur" : "Ensuite, le facteur")
                    .append(" « ").append(ExplanationFacts.label(factor.feature()).toLowerCase()).append(" »");
            if (!factor.feature().equals("is_playoff")) {
                text.append(" (").append(ExplanationFacts.value(factor.feature(), factor.homeValue())).append(" pour ")
                        .append(request.homeTeam()).append(", ").append(ExplanationFacts.value(factor.feature(), factor.awayValue()))
                        .append(" pour ").append(request.awayTeam()).append(')');
            }
            text.append(" joue en faveur de ").append(beneficiary).append('.');
            first = false;
        }
        if (factors.isEmpty()) {
            text.append(" Les deux équipes arrivent dans une forme très proche : c'est surtout l'avantage du terrain qui compte.");
        }
        ScorePrediction score = prediction.score();
        if (score != null) {
            String leader = score.homeTeamWins() ? request.homeTeam() : request.awayTeam();
            text.append(" Score estimé : ").append(request.homeTeam()).append(' ').append(score.homePoints()).append(", ")
                    .append(request.awayTeam()).append(' ').append(score.awayPoints()).append(", soit ")
                    .append(ExplanationFacts.margin(score.margin())).append(" d'écart pour ").append(leader)
                    .append(" (le modèle se trompe en moyenne de ").append(ExplanationFacts.margin(score.marginError()))
                    .append(" sur l'écart).");
            if (score.homeTeamWins() != homeFavorite) {
                text.append(" Le score estimé donne l'avantage à l'autre équipe : le match s'annonce très serré.");
            }
        }
        return new Explanation(text.toString(), PROVIDER, null);
    }
}
