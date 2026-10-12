package com.np3tech.hoopmind.ml.explanation;

import com.np3tech.hoopmind.ml.PredictionFactor;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Mise en forme, en français, des chiffres d'une prédiction : libellé de chaque variable, valeurs lisibles,
 * facteurs triés du plus au moins influent. Partagée par le prompt des LLM et l'explication sans LLM, pour que
 * les deux disent la même chose.
 */
public final class ExplanationFacts {

    private static final Locale FRENCH = Locale.FRANCE;

    private ExplanationFacts() {
    }

    /** Facteurs du plus influent au moins influent (valeur absolue de la contribution). */
    public static List<PredictionFactor> byInfluence(List<PredictionFactor> factors) {
        return factors.stream()
                .sorted(Comparator.comparingDouble((PredictionFactor factor) -> Math.abs(factor.contribution())).reversed())
                .toList();
    }

    public static String label(String feature) {
        return switch (feature) {
            case "win_pct_diff" -> "Pourcentage de victoires cette saison";
            case "last10_win_pct_diff" -> "Victoires sur les 10 derniers matchs";
            case "net_rating_diff" -> "Net rating (points marqués moins points encaissés pour 100 possessions)";
            case "rest_days_diff" -> "Jours de repos avant le match";
            case "is_playoff" -> "Match de playoffs";
            default -> feature;
        };
    }

    /** Valeur d'un côté, lisible : « 62 % », « +4,3 », « 2 jours », « oui ». */
    public static String value(String feature, double value) {
        return switch (feature) {
            case "win_pct_diff", "last10_win_pct_diff" -> percent(value);
            case "net_rating_diff" -> String.format(FRENCH, "%+.1f", value);
            case "rest_days_diff" -> {
                long days = Math.round(value);
                yield days + (days > 1 ? " jours" : " jour");
            }
            case "is_playoff" -> value > 0 ? "oui" : "non";
            default -> String.format(FRENCH, "%.2f", value);
        };
    }

    public static String percent(double probability) {
        return String.format(FRENCH, "%.0f %%", probability * 100);
    }

    public static String signed(double number) {
        return String.format(FRENCH, "%+.2f", number);
    }
}
