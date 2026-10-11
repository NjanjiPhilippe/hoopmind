package com.np3tech.hoopmind.ml.spark;

import com.np3tech.hoopmind.ml.scoring.ScoringCard;
import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.classification.LogisticRegression;
import org.apache.spark.ml.classification.LogisticRegressionModel;
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator;
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator;
import org.apache.spark.ml.feature.StandardScaler;
import org.apache.spark.ml.feature.StandardScalerModel;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.np3tech.hoopmind.ml.spark.GameFeatureBuilder.FEATURES;
import static com.np3tech.hoopmind.ml.spark.GameFeatureBuilder.LABEL;
import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.when;

/**
 * Entraîne et évalue le modèle de prédiction du vainqueur d'un match (régression logistique, dont les probabilités
 * sont bien calibrées et les poids lisibles).
 * <p>
 * L'évaluation respecte le temps : le modèle apprend sur les saisons les plus anciennes et il est testé sur la plus
 * récente, comme s'il avait été entraîné avant qu'elle commence. Sa précision est comparée à deux règles simples
 * (« l'équipe à domicile gagne », « l'équipe au meilleur bilan gagne ») pour mesurer ce qu'il apporte vraiment.
 */
public final class GameOutcomeTrainer {

    /** En dessous, le bilan d'une équipe n'est pas encore représentatif : ces matchs sont exclus de l'entraînement. */
    static final int MIN_PRIOR_GAMES = 5;

    private GameOutcomeTrainer() {
    }

    public record Evaluation(List<String> trainingSeasons, String testSeason, long trainingGames, long testGames,
                             long correctPredictions, double accuracy, double auc, double logLoss,
                             double precision, double recall, double f1,
                             double homeTeamBaseline, double betterRecordBaseline,
                             Map<String, Long> confusionMatrix, Map<String, Double> featureWeights) {
    }

    public record Result(PipelineModel model, Evaluation evaluation) {
    }

    /**
     * @param games lignes produites par {@link GameFeatureBuilder#gameFeatures}
     */
    public static Result train(Dataset<Row> games) {
        Dataset<Row> usable = games
                .filter(col(LABEL).isNotNull())
                .filter(col("home_games_played").geq(MIN_PRIOR_GAMES).and(col("away_games_played").geq(MIN_PRIOR_GAMES)))
                .cache();
        List<String> seasons = usable.select("season").distinct().orderBy("season").as(
                org.apache.spark.sql.Encoders.STRING()).collectAsList();
        if (seasons.isEmpty() || usable.count() < 20) {
            throw new IllegalStateException("Pas assez de matchs joués pour entraîner le modèle.");
        }

        Dataset<Row> train;
        Dataset<Row> test;
        String testSeason;
        if (seasons.size() >= 2) {
            testSeason = seasons.getLast();
            train = usable.filter(col("season").notEqual(testSeason));
            test = usable.filter(col("season").equalTo(testSeason));
        } else {
            // Une seule saison : pas de découpage dans le temps possible, on garde 20 % des matchs au hasard.
            testSeason = seasons.getFirst() + " (20 % des matchs)";
            Dataset<Row>[] split = usable.randomSplit(new double[]{0.8, 0.2}, 42L);
            train = split[0];
            test = split[1];
        }

        PipelineModel model = pipeline().fit(train);
        Dataset<Row> predictions = model.transform(test).cache();
        Evaluation evaluation = evaluate(predictions, model,
                seasons.size() >= 2 ? seasons.subList(0, seasons.size() - 1) : seasons, testSeason, train.count());
        predictions.unpersist();
        usable.unpersist();
        return new Result(model, evaluation);
    }

    static Pipeline pipeline() {
        VectorAssembler assembler = new VectorAssembler()
                .setInputCols(FEATURES)
                .setOutputCol("raw_features");
        // Mettre les variables à la même échelle rend les poids de la régression comparables entre eux.
        StandardScaler scaler = new StandardScaler()
                .setInputCol("raw_features")
                .setOutputCol("features")
                .setWithMean(true)
                .setWithStd(true);
        LogisticRegression regression = new LogisticRegression()
                .setFeaturesCol("features")
                .setLabelCol(LABEL)
                .setMaxIter(100)
                .setRegParam(0.01);
        return new Pipeline().setStages(new PipelineStage[]{assembler, scaler, regression});
    }

    private static Evaluation evaluate(Dataset<Row> predictions, PipelineModel model, List<String> trainingSeasons,
                                       String testSeason, long trainingGames) {
        long total = predictions.count();
        long truePositives = count(predictions, 1, 1);
        long falsePositives = count(predictions, 1, 0);
        long trueNegatives = count(predictions, 0, 0);
        long falseNegatives = count(predictions, 0, 1);
        long correct = truePositives + trueNegatives;

        double precision = ratio(truePositives, truePositives + falsePositives);
        double recall = ratio(truePositives, truePositives + falseNegatives);
        double f1 = precision + recall == 0 ? 0 : 2 * precision * recall / (precision + recall);

        double auc = new BinaryClassificationEvaluator()
                .setLabelCol(LABEL).setRawPredictionCol("rawPrediction").setMetricName("areaUnderROC")
                .evaluate(predictions);
        double logLoss = new MulticlassClassificationEvaluator()
                .setLabelCol(LABEL).setProbabilityCol("probability").setMetricName("logLoss")
                .evaluate(predictions);

        long homeWins = predictions.filter(col(LABEL).equalTo(1.0)).count();
        long betterRecordRight = predictions
                .withColumn("record_pick", when(col("home_win_pct").geq(col("away_win_pct")), 1.0).otherwise(0.0))
                .filter(col("record_pick").equalTo(col(LABEL)))
                .count();

        Map<String, Long> confusion = new LinkedHashMap<>();
        confusion.put("homeWinPredictedCorrectly", truePositives);
        confusion.put("homeWinPredictedWrongly", falsePositives);
        confusion.put("awayWinPredictedCorrectly", trueNegatives);
        confusion.put("awayWinPredictedWrongly", falseNegatives);

        return new Evaluation(trainingSeasons, testSeason, trainingGames, total, correct, ratio(correct, total), auc, logLoss,
                precision, recall, f1, ratio(homeWins, total), ratio(betterRecordRight, total), confusion, weights(model));
    }

    /**
     * Extrait du pipeline entraîné ce qu'il faut pour prédire sans Spark : standardisation et régression.
     */
    static ScoringCard scoringCard(PipelineModel model, String name, String version) {
        StandardScalerModel scaler = (StandardScalerModel) model.stages()[1];
        LogisticRegressionModel regression = (LogisticRegressionModel) model.stages()[2];
        return new ScoringCard(name, version, List.of(FEATURES), scaler.mean().toArray(), scaler.std().toArray(),
                regression.coefficients().toArray(), regression.intercept());
    }

    /** Poids de chaque variable (sur l'échelle standardisée), du plus influent au moins influent. */
    static Map<String, Double> weights(PipelineModel model) {
        LogisticRegressionModel regression = (LogisticRegressionModel) model.stages()[model.stages().length - 1];
        double[] coefficients = regression.coefficients().toArray();
        Map<String, Double> weights = new LinkedHashMap<>();
        java.util.stream.IntStream.range(0, FEATURES.length).boxed()
                .sorted((a, b) -> Double.compare(Math.abs(coefficients[b]), Math.abs(coefficients[a])))
                .forEach(i -> weights.put(FEATURES[i], coefficients[i]));
        return weights;
    }

    private static long count(Dataset<Row> predictions, double predicted, double actual) {
        return predictions.filter(col("prediction").equalTo(predicted).and(col(LABEL).equalTo(actual))).count();
    }

    private static double ratio(long numerator, long denominator) {
        return denominator == 0 ? 0 : (double) numerator / denominator;
    }
}
