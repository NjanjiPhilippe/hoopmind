package com.np3tech.hoopmind.ml.explanation;

import org.junit.jupiter.api.Test;

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
}
