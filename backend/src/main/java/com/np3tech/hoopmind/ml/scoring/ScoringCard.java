package com.np3tech.hoopmind.ml.scoring;

import java.util.Arrays;
import java.util.List;

/**
 * Tout ce qu'il faut pour appliquer une régression entraînée par Spark (logistique pour le vainqueur, linéaire pour
 * le score), sans Spark : moyennes et écarts-types de
 * la standardisation, poids et ordonnée à l'origine. Enregistrée en JSON à chaque entraînement.
 *
 * @param features variables dans l'ordre des tableaux ({@link GameOutcomeFeatures#NAMES} ou {@link GameScoreFeatures#NAMES})
 */
public record ScoringCard(String model, String version, List<String> features, double[] means, double[] stds,
                          double[] coefficients, double intercept) {

    public ScoringCard {
        features = List.copyOf(features);
        int size = features.size();
        if (means.length != size || stds.length != size || coefficients.length != size) {
            throw new IllegalArgumentException("Carte de score incohérente : " + size + " variables");
        }
        means = means.clone();
        stds = stds.clone();
        coefficients = coefficients.clone();
    }

    /** Probabilité de victoire de l'équipe à domicile, calculée comme le pipeline Spark (StandardScaler puis sigmoïde). */
    public double homeWinProbability(double[] features) {
        return 1 / (1 + Math.exp(-linearPrediction(features)));
    }

    /** Valeur prédite par une régression linéaire (modèle de score) : ordonnée à l'origine plus les contributions. */
    public double linearPrediction(double[] features) {
        double value = intercept;
        for (double contribution : contributions(features)) {
            value += contribution;
        }
        return value;
    }

    /**
     * Part de chaque variable dans la marge (log-cote) : poids × valeur standardisée. Leur somme plus
     * l'ordonnée à l'origine donne la marge, donc la probabilité. Positive : en faveur de l'équipe à domicile.
     */
    public double[] contributions(double[] features) {
        if (features.length != coefficients.length) {
            throw new IllegalArgumentException("Attendu " + coefficients.length + " variables, reçu " + features.length);
        }
        double[] contributions = new double[features.length];
        for (int i = 0; i < features.length; i++) {
            // Spark : une variable constante à l'entraînement (écart-type nul) vaut 0 après standardisation.
            double scaled = stds[i] == 0 ? 0 : (features[i] - means[i]) / stds[i];
            contributions[i] = coefficients[i] * scaled;
        }
        return contributions;
    }

    public boolean matches(List<String> expectedFeatures) {
        return features.equals(expectedFeatures);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ScoringCard card && model.equals(card.model) && version.equals(card.version)
                && features.equals(card.features) && Arrays.equals(means, card.means) && Arrays.equals(stds, card.stds)
                && Arrays.equals(coefficients, card.coefficients) && intercept == card.intercept;
    }

    @Override
    public int hashCode() {
        return version.hashCode();
    }

    @Override
    public String toString() {
        return "ScoringCard[" + model + " " + version + ", " + features + "]";
    }
}
