package com.np3tech.hoopmind.ml.spark;

import com.np3tech.hoopmind.ml.spark.GameScoreTrainer.Evaluation;
import org.apache.spark.sql.Row;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.np3tech.hoopmind.ml.spark.SparkTestSession.teamGame;
import static org.assertj.core.api.Assertions.assertThat;

class GameScoreTrainerTests {

    /**
     * Trois saisons simulées : chaque équipe a une attaque et une défense (mélangées à chaque saison), l'équipe à
     * domicile marque 3 points de plus en moyenne, et du hasard. Le modèle doit retrouver le niveau des équipes dans
     * leurs moyennes de points et battre la règle simple, qui ignore l'avantage du terrain.
     */
    @Test
    void predictsBothScoresAndBeatsTheBaselineOnTheMargin() {
        Random random = new Random(5);
        List<Row> rows = new ArrayList<>();
        long gameId = 0;
        for (int season = 0; season < 3; season++) {
            String name = (2021 + season) + "-" + (2022 + season);
            double[] offense = random.doubles(10, -8, 8).toArray();
            double[] defense = random.doubles(10, -8, 8).toArray();
            LocalDate day = LocalDate.of(2021 + season, 10, 20);
            for (int round = 0; round < 4; round++) {
                for (int home = 0; home < 10; home++) {
                    for (int away = 0; away < 10; away++) {
                        if (home == away) continue;
                        int homePoints = (int) Math.round(111.5 + offense[home] - defense[away] + random.nextGaussian() * 8);
                        int awayPoints = (int) Math.round(108.5 + offense[away] - defense[home] + random.nextGaussian() * 8);
                        if (homePoints == awayPoints) homePoints++;
                        gameId++;
                        day = day.plusDays(gameId % 3 == 0 ? 1 : 0);
                        rows.add(teamGame(gameId, name, day, home, true, homePoints, awayPoints));
                        rows.add(teamGame(gameId, name, day, away, false, awayPoints, homePoints));
                    }
                }
            }
        }

        GameScoreTrainer.Result result = GameScoreTrainer.train(GameFeatureBuilder.gameFeatures(
                SparkTestSession.get().createDataFrame(rows, GameFeatureBuilder.TEAM_GAME_SCHEMA)));
        Evaluation evaluation = result.evaluation();

        assertThat(evaluation.testSeason()).isEqualTo("2023-2024");
        assertThat(evaluation.trainingSeasons()).containsExactly("2021-2022", "2022-2023");
        assertThat(evaluation.testGames()).isGreaterThan(300);
        assertThat(evaluation.marginMeanAbsoluteError()).isLessThan(evaluation.baselineMarginMeanAbsoluteError());
        // Le hasard seul (deux tirages à 8 points d'écart-type) laisse environ 9 points d'erreur sur l'écart.
        assertThat(evaluation.marginMeanAbsoluteError()).isBetween(7.0, 12.0);
        assertThat(evaluation.marginRootMeanSquaredError()).isGreaterThanOrEqualTo(evaluation.marginMeanAbsoluteError());
        assertThat(evaluation.pointsMeanAbsoluteError()).isBetween(5.0, 9.0);
        assertThat(evaluation.winnerAccuracy()).isGreaterThan(0.6);
        assertThat(evaluation.correctWinners()).isEqualTo(Math.round(evaluation.winnerAccuracy() * evaluation.testGames()));
    }
}
