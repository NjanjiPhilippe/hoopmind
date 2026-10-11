package com.np3tech.hoopmind.ml.scoring;

import java.util.Arrays;
import java.util.List;

/**
 * Tout ce qu'il faut pour appliquer la régression logistique entraînée, sans Spark : moyennes et écarts-types de
 * la standardisation, poids et ordonnée à l'origine. Enregistrée en JSON à chaque entraînement.
 *
 * @param features variables dans l'ordre des tableaux (doit être {@link GameOutcomeFeatures#NAMES})
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
        if (features.length != coefficients.length) {
            throw new IllegalArgumentException("Attendu " + coefficients.length + " variables, reçu " + features.length);
        }
        double margin = intercept;
        for (int i = 0; i < features.length; i++) {
            // Spark : une variable constante à l'entraînement (écart-type nul) vaut 0 après standardisation.
            double scaled = stds[i] == 0 ? 0 : (features[i] - means[i]) / stds[i];
            margin += coefficients[i] * scaled;
        }
        return 1 / (1 + Math.exp(-margin));
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
