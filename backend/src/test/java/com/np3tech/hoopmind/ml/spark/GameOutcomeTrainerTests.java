package com.np3tech.hoopmind.ml.spark;

import com.np3tech.hoopmind.ml.spark.GameOutcomeTrainer.Evaluation;
import org.apache.spark.sql.Row;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.np3tech.hoopmind.ml.spark.SparkTestSession.teamGame;
import static org.assertj.core.api.Assertions.assertThat;

class GameOutcomeTrainerTests {

    /**
     * Trois saisons simulées : dix équipes de niveaux différents (mélangés à chaque saison), un avantage du terrain,
     * et du hasard. Le modèle doit retrouver le niveau des équipes dans leur forme et battre les règles simples.
     */
    @Test
    void learnsOnPastSeasonsAndBeatsTheBaselinesOnTheLatestOne() {
        Random random = new Random(7);
        List<Row> rows = new ArrayList<>();
        long gameId = 0;
        for (int season = 0; season < 3; season++) {
            String name = (2021 + season) + "-" + (2022 + season);
            double[] strength = random.doubles(10, -8, 8).toArray();
            LocalDate day = LocalDate.of(2021 + season, 10, 20);
            for (int round = 0; round < 4; round++) {
                for (int home = 0; home < 10; home++) {
                    for (int away = 0; away < 10; away++) {
                        if (home == away) continue;
                        double margin = strength[home] - strength[away] + 3 + random.nextGaussian() * 11;
                        int homePoints = 110 + (int) Math.round(margin / 2);
                        int awayPoints = 110 - (int) Math.round(margin / 2);
                        if (homePoints == awayPoints) homePoints++;
                        gameId++;
                        day = day.plusDays(gameId % 3 == 0 ? 1 : 0);
                        rows.add(teamGame(gameId, name, day, home, true, homePoints, awayPoints));
                        rows.add(teamGame(gameId, name, day, away, false, awayPoints, homePoints));
                    }
                }
            }
        }

        GameOutcomeTrainer.Result result = GameOutcomeTrainer.train(GameFeatureBuilder.gameFeatures(
                SparkTestSession.get().createDataFrame(rows, GameFeatureBuilder.TEAM_GAME_SCHEMA)));
        Evaluation evaluation = result.evaluation();

        assertThat(evaluation.testSeason()).isEqualTo("2023-2024");
        assertThat(evaluation.trainingSeasons()).containsExactly("2021-2022", "2022-2023");
        assertThat(evaluation.testGames()).isGreaterThan(300);
        assertThat(evaluation.accuracy()).isGreaterThan(evaluation.homeTeamBaseline());
        assertThat(evaluation.accuracy()).isGreaterThanOrEqualTo(evaluation.betterRecordBaseline() - 0.02);
        assertThat(evaluation.auc()).isGreaterThan(0.7);
        assertThat(evaluation.logLoss()).isLessThan(0.65);
        assertThat(evaluation.featureWeights()).hasSize(GameFeatureBuilder.FEATURES.length);
        assertThat(evaluation.confusionMatrix().values().stream().mapToLong(Long::longValue).sum())
                .isEqualTo(evaluation.testGames());
    }
}
