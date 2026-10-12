package com.np3tech.hoopmind.ml.scoring;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ScoringCardTests {

    private final ScoringCard card = new ScoringCard("logistic-regression", "v1", GameOutcomeFeatures.NAMES,
            new double[]{0, 0, 0, 0, 0.1}, new double[]{0.2, 0.25, 8, 1.5, 0},
            new double[]{0.9, 0.3, 0.6, 0.1, 0.4}, 0.15);

    @Test
    void contributionsAndInterceptGiveTheProbability() {
        double[] features = {0.2, -0.1, 4.0, 1.0, 0.0};

        double[] contributions = card.contributions(features);

        assertThat(contributions[0]).isCloseTo(0.9 * 0.2 / 0.2, within(1e-12));
        assertThat(contributions[1]).as("défavorable à l'équipe à domicile").isNegative();
        assertThat(contributions[4]).as("variable constante à l'entraînement").isZero();
        double margin = card.intercept() + Arrays.stream(contributions).sum();
        assertThat(card.homeWinProbability(features)).isCloseTo(1 / (1 + Math.exp(-margin)), within(1e-12));
    }
}
