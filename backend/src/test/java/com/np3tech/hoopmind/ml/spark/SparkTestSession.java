package com.np3tech.hoopmind.ml.spark;

import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;

import java.sql.Date;
import java.time.LocalDate;

/** Session Spark locale partagée par les tests du pipeline ML (sans Spring ni base de données). */
final class SparkTestSession {

    private SparkTestSession() {
    }

    static SparkSession get() {
        return SparkSession.builder()
                .master("local[2]")
                .appName("hoopmind-tests")
                .config("spark.ui.enabled", "false")
                .config("spark.sql.shuffle.partitions", "4")
                // Sinon Spark installe sa fabrique d'URL dans la JVM partagée par tous les tests, et le Tomcat d'un
                // test lancé ensuite sur un vrai port ne peut plus installer la sienne (« factory already defined »).
                .config("spark.sql.defaultUrlStreamHandlerFactory.enabled", "false")
                .getOrCreate();
    }

    /** Une ligne équipe-match au format {@link GameFeatureBuilder#TEAM_GAME_SCHEMA}, avec 100 possessions par équipe. */
    static Row teamGame(long gameId, String season, LocalDate date, long teamId, boolean home,
                        Integer points, Integer pointsAllowed) {
        Integer won = points == null ? null : points > pointsAllowed ? 1 : 0;
        Double possessions = points == null ? null : 100.0;
        return RowFactory.create(gameId, season, Date.valueOf(date), teamId, home ? 1 : 0, 0, won, points, pointsAllowed,
                possessions, possessions);
    }
}
