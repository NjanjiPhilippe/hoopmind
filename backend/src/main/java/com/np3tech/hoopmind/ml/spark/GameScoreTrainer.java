package com.np3tech.hoopmind.ml.spark;

import com.np3tech.hoopmind.ml.scoring.GameScoreCard;
import com.np3tech.hoopmind.ml.scoring.ScoringCard;
import com.np3tech.hoopmind.ml.spark.GameOutcomeTrainer.Split;
import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.feature.StandardScaler;
import org.apache.spark.ml.feature.StandardScalerModel;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.ml.regression.LinearRegression;
import org.apache.spark.ml.regression.LinearRegressionModel;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.types.DataTypes;

import java.util.List;

import static com.np3tech.hoopmind.ml.spark.GameFeatureBuilder.SCORE_FEATURES;
import static org.apache.spark.sql.functions.abs;
import static org.apache.spark.sql.functions.avg;
import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.count;
import static org.apache.spark.sql.functions.pow;
import static org.apache.spark.sql.functions.sqrt;
import static org.apache.spark.sql.functions.sum;
import static org.apache.spark.sql.functions.when;

/**
 * Entraîne et évalue le modèle de score : deux régressions linéaires, une pour les points de l'équipe à domicile, une
 * pour ceux de l'équipe à l'extérieur, sur les moyennes de points des deux équipes avant le match. L'écart et le total
 * s'en déduisent. Mêmes matchs et même découpage dans le temps que le modèle du vainqueur.
 * <p>
 * Le modèle est comparé à une règle simple : chaque équipe marque la moyenne de ce qu'elle marque et de ce que
 * l'adversaire encaisse, sans avantage du terrain.
 */
public final class GameScoreTrainer {

    static final String HOME_LABEL = "home_points_label";
    static final String AWAY_LABEL = "away_points_label";
    static final String HOME_PREDICTION = "home_points_prediction";
    static final String AWAY_PREDICTION = "away_points_prediction";

    private GameScoreTrainer() {
    }

    /**
     * Erreurs mesurées sur la saison de test, en points.
     *
     * @param winnerAccuracy            part des matchs où l'écart prédit désigne le bon vainqueur
     * @param pointsMeanAbsoluteError   erreur moyenne sur les points d'une équipe
     * @param marginMeanAbsoluteError   erreur moyenne sur l'écart
     * @param baselineMarginMeanAbsoluteError erreur moyenne sur l'écart de la règle simple
     */
    public record Evaluation(List<String> trainingSeasons, String testSeason, long trainingGames, long testGames,
                             long correctWinners, double winnerAccuracy, double pointsMeanAbsoluteError,
                             double marginMeanAbsoluteError, double marginRootMeanSquaredError,
                             double totalMeanAbsoluteError, double baselineMarginMeanAbsoluteError) {
    }

    public record Result(PipelineModel model, Evaluation evaluation) {
    }

    /**
     * @param games lignes produites par {@link GameFeatureBuilder#gameFeatures}
     */
    public static Result train(Dataset<Row> games) {
        Dataset<Row> usable = GameOutcomeTrainer.usable(games
                .withColumn(HOME_LABEL, col("home_points").cast(DataTypes.DoubleType))
                .withColumn(AWAY_LABEL, col("away_points").cast(DataTypes.DoubleType)));
        Split split = GameOutcomeTrainer.split(usable);
        PipelineModel model = pipeline().fit(split.train());
        Dataset<Row> predictions = model.transform(split.test()).cache();
        Evaluation evaluation = evaluate(predictions, split, split.train().count());
        predictions.unpersist();
        usable.unpersist();
        return new Result(model, evaluation);
    }

    static Pipeline pipeline() {
        VectorAssembler assembler = new VectorAssembler()
                .setInputCols(SCORE_FEATURES)
                .setOutputCol("raw_score_features");
        StandardScaler scaler = new StandardScaler()
                .setInputCol("raw_score_features")
                .setOutputCol("score_features")
                .setWithMean(true)
                .setWithStd(true);
        return new Pipeline().setStages(new PipelineStage[]{assembler, scaler,
                regression(HOME_LABEL, HOME_PREDICTION), regression(AWAY_LABEL, AWAY_PREDICTION)});
    }

    private static LinearRegression regression(String label, String prediction) {
        return new LinearRegression()
                .setFeaturesCol("score_features")
                .setLabelCol(label)
                .setPredictionCol(prediction)
                .setMaxIter(100)
                .setRegParam(0.01);
    }

    private static Evaluation evaluate(Dataset<Row> predictions, Split split, long trainingGames) {
        Column predictedMargin = col(HOME_PREDICTION).minus(col(AWAY_PREDICTION));
        Column actualMargin = col(HOME_LABEL).minus(col(AWAY_LABEL));
        Column baselineMargin = col("home_points_per_game").plus(col("away_points_allowed_per_game")).divide(2)
                .minus(col("away_points_per_game").plus(col("home_points_allowed_per_game")).divide(2));
        Row row = predictions.agg(
                count("*"),
                sum(when(predictedMargin.gt(0).equalTo(actualMargin.gt(0)), 1).otherwise(0)),
                avg(abs(col(HOME_PREDICTION).minus(col(HOME_LABEL))).plus(abs(col(AWAY_PREDICTION).minus(col(AWAY_LABEL))))
                        .divide(2)),
                avg(abs(predictedMargin.minus(actualMargin))),
                sqrt(avg(pow(predictedMargin.minus(actualMargin), 2))),
                avg(abs(col(HOME_PREDICTION).plus(col(AWAY_PREDICTION)).minus(col(HOME_LABEL).plus(col(AWAY_LABEL))))),
                avg(abs(baselineMargin.minus(actualMargin)))
        ).first();
        long total = row.getLong(0);
        long correct = row.isNullAt(1) ? 0 : row.getLong(1);
        return new Evaluation(split.trainingSeasons(), split.testSeason(), trainingGames, total, correct,
                total == 0 ? 0 : (double) correct / total, number(row, 2), number(row, 3), number(row, 4),
                number(row, 5), number(row, 6));
    }

    /** Extrait du pipeline entraîné les deux régressions, appliquées ensuite sans Spark. */
    static GameScoreCard scoreCard(PipelineModel model, String name, String version, double marginError) {
        StandardScalerModel scaler = (StandardScalerModel) model.stages()[1];
        return new GameScoreCard(name, version,
                card(scaler, (LinearRegressionModel) model.stages()[2], name, version),
                card(scaler, (LinearRegressionModel) model.stages()[3], name, version),
                marginError);
    }

    private static ScoringCard card(StandardScalerModel scaler, LinearRegressionModel regression, String name,
                                    String version) {
        return new ScoringCard(name, version, List.of(SCORE_FEATURES), scaler.mean().toArray(), scaler.std().toArray(),
                regression.coefficients().toArray(), regression.intercept());
    }

    private static double number(Row row, int index) {
        return row.isNullAt(index) ? 0 : row.getDouble(index);
    }
}
