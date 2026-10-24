package com.np3tech.hoopmind.ml.explanation;

import com.np3tech.hoopmind.ml.GameOutcomeAnalysis;
import com.np3tech.hoopmind.ml.GameOutcomePrediction;
import com.np3tech.hoopmind.ml.PredictionFactor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateExplanationTests {

    @Test
    void namesTheFavoriteAndTheFactorsThatMatter() {
        Explanation explanation = TemplateExplanation.of(Fixtures.request());

        assertThat(explanation.provider()).isEqualTo("template");
        assertThat(explanation.model()).isNull();
        assertThat(explanation.text())
                .startsWith("Le modèle voit Boston Celtics gagner, avec 71 % de chances.")
                .contains("Le facteur le plus important « net rating")
                .contains("(+9,4 pour Boston Celtics, +1,2 pour New York Knicks) joue en faveur de Boston Celtics.")
                .contains("En revanche, le facteur « jours de repos avant le match » (1 jour pour Boston Celtics, "
                        + "3 jours pour New York Knicks) joue en faveur de New York Knicks.")
                .doesNotContain("10 derniers matchs")
                .doesNotContain("playoffs")
                .doesNotContain("très ouvert");
    }

    /** Données réelles d'un essai : le modèle compense le net rating par le bilan, de sens contraire. */
    @Test
    void neverPresentsAModelCorrectionAsAnAdvantage() {
        GameOutcomeAnalysis analysis = new GameOutcomeAnalysis(
                new GameOutcomePrediction(true, 0.97, 0.03, "logistic-regression"), "v1", 0.2,
                List.of(new PredictionFactor("net_rating_diff", 11.0, -14.5, 2.87),
                        new PredictionFactor("last10_win_pct_diff", 0.6, 0.1, 0.71),
                        new PredictionFactor("win_pct_diff", 0.74, 0.07, -0.38),
                        new PredictionFactor("rest_days_diff", 1, 2, -0.04),
                        new PredictionFactor("is_playoff", 0, 0, 0)), 30, 30);

        String text = TemplateExplanation.of(new ExplanationRequest("Boston Celtics", "San Antonio Spurs", Fixtures.DAY,
                false, analysis)).text();

        assertThat(text).contains("net rating", "10 derniers matchs")
                .doesNotContain("pourcentage de victoires")
                .doesNotContain("en faveur de San Antonio Spurs");
    }
}
