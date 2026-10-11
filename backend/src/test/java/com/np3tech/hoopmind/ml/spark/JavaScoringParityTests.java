package com.np3tech.hoopmind.ml.spark;

import com.np3tech.hoopmind.ml.scoring.GameOutcomeFeatures;
import com.np3tech.hoopmind.ml.scoring.ScoringCard;
import com.np3tech.hoopmind.ml.scoring.TeamForm;
import com.np3tech.hoopmind.nba.GameRecord;
import com.np3tech.hoopmind.nba.GameRecord.BoxScore;
import org.apache.spark.ml.linalg.Vector;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * La prédiction se fait en Java, l'entraînement avec Spark : ces tests vérifient que les deux donnent exactement
 * les mêmes variables et les mêmes probabilités (aucun écart entre entraînement et production).
 */
class JavaScoringParityTests {

    private final List<GameRecord> history = simulatedSeasons();

    @Test
    void javaFormMatchesSparkFeaturesForEveryGame() {
        Dataset<Row> games = GameFeatureBuilder.gameFeatures(teamGames(history));
        Map<String, List<GameRecord>> bySeason = history.stream().collect(Collectors.groupingBy(GameRecord::season));
        Map<Long, GameRecord> byId = history.stream().collect(Collectors.toMap(GameRecord::gameId, Function.identity()));

        List<Row> rows = games.collectAsList();
        assertThat(rows).hasSize(history.size());
        for (Row row : rows) {
            GameRecord game = byId.get(row.<Long>getAs("game_id"));
            List<GameRecord> season = bySeason.get(game.season());
            double[] java = GameOutcomeFeatures.of(
                    TeamForm.before(season, game.homeTeamId(), game.date(), game.gameId()),
                    TeamForm.before(season, game.awayTeamId(), game.date(), game.gameId()),
                    game.playoff());
            for (int i = 0; i < java.length; i++) {
                String feature = GameOutcomeFeatures.NAMES.get(i);
                assertThat(java[i]).as("%s du match %d", feature, game.gameId())
                        .isCloseTo(row.<Double>getAs(feature), within(1e-9));
            }
        }
    }

    @Test
    void scoringCardGivesTheSameProbabilitiesAsTheSparkPipeline() {
        GameOutcomeTrainer.Result result = GameOutcomeTrainer.train(GameFeatureBuilder.gameFeatures(teamGames(history)));
        ScoringCard card = GameOutcomeTrainer.scoringCard(result.model(), "test", "v1");

        List<Row> predictions = result.model().transform(GameFeatureBuilder.gameFeatures(teamGames(history))
                        .filter("home_won IS NOT NULL"))
                .select("raw_features", "probability").collectAsList();
        assertThat(predictions).hasSizeGreaterThan(500);
        for (Row prediction : predictions) {
            double[] features = prediction.<Vector>getAs("raw_features").toArray();
            double spark = prediction.<Vector>getAs("probability").apply(1);
            assertThat(card.homeWinProbability(features)).isCloseTo(spark, within(1e-9));
        }
    }

    private static Dataset<Row> teamGames(List<GameRecord> games) {
        List<Row> rows = games.stream()
                .flatMap(game -> Stream.of(TeamGameLoader.teamRow(game, true), TeamGameLoader.teamRow(game, false)))
                .toList();
        return SparkTestSession.get().createDataFrame(rows, GameFeatureBuilder.TEAM_GAME_SCHEMA);
    }

    /**
     * Trois saisons de dix équipes, avec ce qui complique le calcul : plusieurs matchs le même jour, des feuilles de
     * match manquantes, des playoffs en fin de saison et quelques matchs pas encore joués.
     */
    private static List<GameRecord> simulatedSeasons() {
        Random random = new Random(11);
        List<GameRecord> games = new ArrayList<>();
        long id = 0;
        for (int season = 0; season < 3; season++) {
            int year = 2021 + season;
            String name = year + "-" + (year + 1) % 100;
            double[] strength = random.doubles(10, -8, 8).toArray();
            LocalDate day = LocalDate.of(year, 10, 20);
            for (int round = 0; round < 4; round++) {
                for (int home = 0; home < 10; home++) {
                    for (int away = 0; away < 10; away++) {
                        if (home == away) continue;
                        id++;
                        day = day.plusDays(random.nextInt(3));
                        boolean playoff = round == 3 && home < 4 && away < 4;
                        boolean upcoming = season == 2 && round == 3 && home >= 8;
                        double margin = strength[home] - strength[away] + 3 + random.nextGaussian() * 11;
                        int homePoints = 110 + (int) Math.round(margin / 2);
                        int awayPoints = 110 - (int) Math.round(margin / 2);
                        if (homePoints == awayPoints) homePoints++;
                        boolean boxScores = random.nextInt(10) > 0;
                        games.add(new GameRecord(id, name, year, day, home, away, playoff,
                                upcoming ? null : homePoints, upcoming ? null : awayPoints,
                                upcoming || !boxScores ? null : box(random), upcoming || !boxScores ? null : box(random)));
                    }
                }
            }
        }
        return games;
    }

    private static BoxScore box(Random random) {
        return new BoxScore(80 + random.nextInt(15), 8 + random.nextInt(6), 10 + random.nextInt(8), 15 + random.nextInt(15));
    }
}
