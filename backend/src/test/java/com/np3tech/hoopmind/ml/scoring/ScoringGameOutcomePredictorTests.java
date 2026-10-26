package com.np3tech.hoopmind.ml.scoring;

import com.np3tech.hoopmind.ml.MlProperties;
import com.np3tech.hoopmind.ml.ScorePrediction;
import com.np3tech.hoopmind.nba.GameHistory;
import com.np3tech.hoopmind.nba.GameRecord;
import com.np3tech.hoopmind.nba.GameRecord.BoxScore;
import com.np3tech.hoopmind.shared.exception.AppException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class ScoringGameOutcomePredictorTests {

    private static final long CELTICS = 1;
    private static final long LAKERS = 2;
    private static final LocalDate DAY = LocalDate.of(2024, 10, 22);
    private static final BoxScore BOX = new BoxScore(90, 10, 20, 0);

    private final MeterRegistry registry = new SimpleMeterRegistry();

    @TempDir
    Path modelDir;

    @Test
    void measuresEachPredictionAndItsProbability() {
        ScoringCardStore cards = store();
        cards.save(new ScoringCard("logistic-regression", "v7", GameOutcomeFeatures.NAMES,
                new double[]{0, 0, 0, 0, 0.1}, new double[]{0.2, 0.25, 8, 1.5, 0},
                new double[]{0.9, 0.3, 0.6, 0.1, 0.4}, 0.15));
        ScoringGameOutcomePredictor predictor = new ScoringGameOutcomePredictor(new Season(), cards, registry);

        double probability = predictor.predict(10, CELTICS, LAKERS, DAY.plusDays(5), false).homeWinProbability();

        assertThat(registry.get("hoopmind.ml.predictions").tag("model_version", "v7").tag("outcome", "ok")
                .timer().count()).isEqualTo(1);
        assertThat(registry.get("hoopmind.ml.home.win.probability").tag("model_version", "v7").summary().totalAmount())
                .isEqualTo(probability);
    }

    @Test
    void predictsTheScoreOnceTheScoreModelIsTrained() {
        ScoringCardStore cards = store();
        cards.save(new ScoringCard("logistic-regression", "v7", GameOutcomeFeatures.NAMES,
                new double[]{0, 0, 0, 0, 0.1}, new double[]{0.2, 0.25, 8, 1.5, 0},
                new double[]{0.9, 0.3, 0.6, 0.1, 0.4}, 0.15));
        ScoringGameOutcomePredictor predictor = new ScoringGameOutcomePredictor(new Season(), cards, registry);
        assertThat(predictor.predict(10, CELTICS, LAKERS, DAY.plusDays(5), false).score())
                .as("modèle entraîné avant l'ajout du score").isNull();

        // Sans standardisation (moyennes 0, écarts-types 1) : points = ordonnée + somme des poids × variables.
        double[] zeros = new double[GameScoreFeatures.NAMES.size()];
        double[] ones = {1, 1, 1, 1, 1, 1};
        cards.save(new GameScoreCard("linear-regression", "v7",
                new ScoringCard("linear-regression", "v7", GameScoreFeatures.NAMES, zeros, ones,
                        new double[]{0.5, 0, 0, 0.5, 0, 0}, 1.5),
                new ScoringCard("linear-regression", "v7", GameScoreFeatures.NAMES, zeros, ones,
                        new double[]{0, 0.5, 0.5, 0, 0, 0}, -1.5),
                11.2));
        ScorePrediction score = predictor.predict(10, CELTICS, LAKERS, DAY.plusDays(5), false).score();

        // Le faux historique renvoie les deux mêmes matchs pour chaque saison : Celtics 112,5 marqués et 105 encaissés
        // par match, cette saison comme la précédente ; les Lakers l'inverse.
        double celticsScored = 112.5;
        double celticsAllowed = 105.0;
        assertThat(score.homePointsPerGame()).isEqualTo(celticsScored);
        assertThat(score.awayPointsPerGame()).isEqualTo(celticsAllowed);
        assertThat(score.margin()).isCloseTo(3.0 + celticsScored - celticsAllowed, within(1e-9));
        assertThat(score.homePoints()).isEqualTo((int) Math.round(1.5 + celticsScored));
        assertThat(score.awayPoints()).isEqualTo((int) Math.round(-1.5 + celticsAllowed));
        assertThat(score.marginError()).isEqualTo(11.2);
        assertThat(registry.get("hoopmind.ml.predicted.margin").tag("model_version", "v7").summary().count()).isEqualTo(1);
    }

    @Test
    void countsFailuresByErrorCode() {
        ScoringGameOutcomePredictor predictor = new ScoringGameOutcomePredictor(new Season(), store(), registry);

        assertThatThrownBy(() -> predictor.predict(10, CELTICS, LAKERS, DAY, false)).isInstanceOf(AppException.class);

        assertThat(registry.get("hoopmind.ml.predictions").tag("outcome", "MODEL_NOT_TRAINED").timer().count())
                .isEqualTo(1);
        assertThat(registry.find("hoopmind.ml.home.win.probability").summary()).isNull();
    }

    private ScoringCardStore store() {
        return new ScoringCardStore(new MlProperties(5, modelDir.toString()), JsonMapper.builder().build());
    }

    private static final class Season implements GameHistory {

        @Override
        public List<GameRecord> teamGames(int seasonYear, Collection<Long> teamIds) {
            return List.of(
                    new GameRecord(1, "2024-25", 2024, DAY, CELTICS, LAKERS, false, 120, 100, BOX, BOX),
                    new GameRecord(2, "2024-25", 2024, DAY.plusDays(2), LAKERS, CELTICS, false, 110, 105, BOX, BOX));
        }

        @Override
        public Optional<Integer> seasonYear(long seasonId) {
            return Optional.of(2024);
        }

        @Override
        public List<Integer> recentSeasonYears(int count) {
            return List.of(2024);
        }

        @Override
        public List<GameRecord> games(Collection<Integer> seasonYears) {
            return List.of();
        }

        @Override
        public Map<Long, String> teamNames(Collection<Long> teamIds) {
            return Map.of();
        }
    }
}
