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

    /**
     * Le facteur pousse vers une équipe alors que ses valeurs sont meilleures pour l'autre. C'est un effet de
     * correction du modèle : quand deux variables mesurent presque la même chose (bilan et net rating), la régression
     * en compense une par l'autre. Le présenter comme un avantage (« 74 % de victoires contre 7 %, en faveur de
     * l'équipe à 7 % ») serait faux pour le lecteur.
     */
    public static boolean counterIntuitive(PredictionFactor factor) {
        if (factor.feature().equals("is_playoff")) {
            return false;
        }
        double difference = factor.homeValue() - factor.awayValue();
        return difference != 0 && factor.contribution() != 0 && Math.signum(difference) != Math.signum(factor.contribution());
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

    /** Points avec une décimale : « 112,4 ». */
    public static String points(double points) {
        return String.format(FRENCH, "%.1f", points);
    }

    /** Écart arrondi, jamais nul : « 1 point », « 7 points ». */
    public static String margin(double margin) {
        long points = Math.max(1, Math.round(Math.abs(margin)));
        return points + (points > 1 ? " points" : " point");
    }

    public static String signed(double number) {
        return String.format(FRENCH, "%+.2f", number);
    }
}
