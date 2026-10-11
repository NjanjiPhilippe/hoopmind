package com.np3tech.hoopmind.ml.spark;

import com.np3tech.hoopmind.ml.scoring.GameOutcomeFeatures;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.expressions.Window;
import org.apache.spark.sql.expressions.WindowSpec;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.util.Arrays;

import static org.apache.spark.sql.functions.avg;
import static org.apache.spark.sql.functions.coalesce;
import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.count;
import static org.apache.spark.sql.functions.datediff;
import static org.apache.spark.sql.functions.lag;
import static org.apache.spark.sql.functions.least;
import static org.apache.spark.sql.functions.lit;
import static org.apache.spark.sql.functions.sum;
import static org.apache.spark.sql.functions.when;

/**
 * Construit les variables d'entrée du modèle de prédiction des matchs avec les fonctions de fenêtre de Spark.
 * <p>
 * Pour chaque match, la forme de chaque équipe est calculée <b>uniquement à partir de ses matchs précédents de la même
 * saison</b> (fenêtre {@code rowsBetween(unboundedPreceding, -1)}). Utiliser le bilan de fin de saison ferait « tricher »
 * le modèle : ce bilan contient déjà le résultat du match à prédire.
 */
public final class GameFeatureBuilder {

    /** Une ligne par équipe et par match (deux lignes par match). */
    public static final StructType TEAM_GAME_SCHEMA = new StructType(new StructField[]{
            DataTypes.createStructField("game_id", DataTypes.LongType, false),
            DataTypes.createStructField("season", DataTypes.StringType, false),
            DataTypes.createStructField("game_date", DataTypes.DateType, false),
            DataTypes.createStructField("team_id", DataTypes.LongType, false),
            DataTypes.createStructField("is_home", DataTypes.IntegerType, false),
            DataTypes.createStructField("is_playoff", DataTypes.IntegerType, false),
            // null tant que le match n'est pas joué
            DataTypes.createStructField("won", DataTypes.IntegerType, true),
            DataTypes.createStructField("points", DataTypes.IntegerType, true),
            DataTypes.createStructField("points_allowed", DataTypes.IntegerType, true),
            // null si la feuille de match manque
            DataTypes.createStructField("possessions", DataTypes.DoubleType, true),
            DataTypes.createStructField("opponent_possessions", DataTypes.DoubleType, true)
    });

    /** Forme d'une équipe avant un match. */
    static final String[] FORM_COLUMNS = {
            "games_played", "win_pct", "last10_win_pct", "points_per_game", "points_allowed_per_game",
            "offensive_rating", "defensive_rating", "net_rating", "rest_days"
    };

    /**
     * Variables d'entrée du modèle, dans l'ordre du VectorAssembler : uniquement des écarts entre les deux équipes.
     * L'avantage du terrain est porté par l'ordonnée à l'origine (l'équipe « home » reçoit toujours).
     * Garder aussi les valeurs de chaque équipe ({@code home_x}, {@code away_x}) en plus de leur écart rendrait les
     * variables colinéaires ({@code x_diff = home_x - away_x}) : poids instables et illisibles.
     */
    public static final String[] FEATURES = GameOutcomeFeatures.NAMES.toArray(String[]::new);

    public static final String LABEL = "home_won";

    /** Valeurs neutres quand une équipe n'a pas encore joué (début de saison). */
    private static final double NEUTRAL_WIN_PCT = 0.5;
    private static final double NEUTRAL_RATING = 110.0;
    private static final double NEUTRAL_POINTS = 110.0;
    private static final int MAX_REST_DAYS = 7;

    private GameFeatureBuilder() {
    }

    /**
     * Une ligne par match avec la forme des deux équipes avant le match, les écarts entre elles et le résultat
     * ({@code home_won}, null pour un match à venir).
     */
    public static Dataset<Row> gameFeatures(Dataset<Row> teamGames) {
        WindowSpec ordered = Window.partitionBy("season", "team_id").orderBy("game_date", "game_id");
        WindowSpec before = ordered.rowsBetween(Window.unboundedPreceding(), -1);
        WindowSpec lastTen = ordered.rowsBetween(-10, -1);

        Dataset<Row> form = teamGames
                .withColumn("box_points", when(col("possessions").isNotNull(), col("points")))
                .withColumn("box_points_allowed", when(col("opponent_possessions").isNotNull(), col("points_allowed")))
                .withColumn("games_played", count("won").over(before))
                .withColumn("win_pct", avg("won").over(before))
                .withColumn("last10_win_pct", avg("won").over(lastTen))
                .withColumn("points_per_game", avg("points").over(before))
                .withColumn("points_allowed_per_game", avg("points_allowed").over(before))
                .withColumn("offensive_rating", rating(sum("box_points").over(before), sum("possessions").over(before)))
                .withColumn("defensive_rating",
                        rating(sum("box_points_allowed").over(before), sum("opponent_possessions").over(before)))
                .withColumn("rest_days", least(
                        coalesce(datediff(col("game_date"), lag("game_date", 1).over(ordered)), lit(MAX_REST_DAYS)),
                        lit(MAX_REST_DAYS)));
        return pairTeams(withNeutralValues(form));
    }

    private static Column rating(Column points, Column possessions) {
        return when(possessions.gt(0), points.multiply(100.0).divide(possessions));
    }

    private static Dataset<Row> withNeutralValues(Dataset<Row> form) {
        return form
                .withColumn("games_played", coalesce(col("games_played"), lit(0L)))
                .withColumn("win_pct", coalesce(col("win_pct"), lit(NEUTRAL_WIN_PCT)))
                .withColumn("last10_win_pct", coalesce(col("last10_win_pct"), lit(NEUTRAL_WIN_PCT)))
                .withColumn("points_per_game", coalesce(col("points_per_game"), lit(NEUTRAL_POINTS)))
                .withColumn("points_allowed_per_game", coalesce(col("points_allowed_per_game"), lit(NEUTRAL_POINTS)))
                .withColumn("offensive_rating", coalesce(col("offensive_rating"), lit(NEUTRAL_RATING)))
                .withColumn("defensive_rating", coalesce(col("defensive_rating"), lit(NEUTRAL_RATING)))
                .withColumn("net_rating", col("offensive_rating").minus(col("defensive_rating")))
                .withColumn("rest_days", col("rest_days").cast(DataTypes.DoubleType));
    }

    private static Dataset<Row> pairTeams(Dataset<Row> form) {
        Dataset<Row> home = prefixed(form.filter(col("is_home").equalTo(1)), "home_")
                .withColumnRenamed("home_game_id", "game_id")
                .withColumnRenamed("home_season", "season")
                .withColumnRenamed("home_game_date", "game_date")
                .withColumnRenamed("home_is_playoff", "is_playoff")
                .withColumn(LABEL, col("home_won").cast(DataTypes.DoubleType));
        Dataset<Row> away = prefixed(form.filter(col("is_home").equalTo(0)), "away_")
                .withColumnRenamed("away_game_id", "game_id");
        return derived(home.join(away, "game_id")
                .withColumn("is_playoff", col("is_playoff").cast(DataTypes.DoubleType)));
    }

    private static Dataset<Row> derived(Dataset<Row> games) {
        return games
                .withColumn("win_pct_diff", col("home_win_pct").minus(col("away_win_pct")))
                .withColumn("last10_win_pct_diff", col("home_last10_win_pct").minus(col("away_last10_win_pct")))
                .withColumn("net_rating_diff", col("home_net_rating").minus(col("away_net_rating")))
                .withColumn("rest_days_diff", col("home_rest_days").minus(col("away_rest_days")));
    }

    private static Dataset<Row> prefixed(Dataset<Row> form, String prefix) {
        Column[] columns = Arrays.stream(form.columns()).map(c -> col(c).alias(prefix + c)).toArray(Column[]::new);
        return form.select(columns);
    }
}
