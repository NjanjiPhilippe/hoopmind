package com.np3tech.hoopmind.ml;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ScorePredictionTests {

    @Test
    void roundsBothScoresAndKeepsTheRawMarginAndTotal() {
        ScorePrediction score = ScorePrediction.of(112.6, 105.2, 10, 0, 0, 0, 0);

        assertThat(score.homePoints()).isEqualTo(113);
        assertThat(score.awayPoints()).isEqualTo(105);
        assertThat(score.margin()).isCloseTo(7.4, within(1e-9));
        assertThat(score.total()).isCloseTo(217.8, within(1e-9));
        assertThat(score.homeTeamWins()).isTrue();
    }

    @Test
    void neverPredictsATie() {
        ScorePrediction home = ScorePrediction.of(110.4, 109.6, 10, 0, 0, 0, 0);
        assertThat(home.homePoints()).isEqualTo(111);
        assertThat(home.awayPoints()).isEqualTo(110);

        ScorePrediction away = ScorePrediction.of(109.6, 110.4, 10, 0, 0, 0, 0);
        assertThat(away.homePoints()).isEqualTo(110);
        assertThat(away.awayPoints()).isEqualTo(111);
        assertThat(away.homeTeamWins()).isFalse();
    }
}
