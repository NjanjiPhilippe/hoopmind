package com.np3tech.hoopmind.ml.spark;

import com.np3tech.hoopmind.nba.GameHistory;
import com.np3tech.hoopmind.nba.GameRecord;
import com.np3tech.hoopmind.nba.GameRecord.BoxScore;
import lombok.RequiredArgsConstructor;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.util.List;
import java.util.stream.Stream;

/**
 * Convertit l'historique exposé par le module NBA ({@link GameHistory}) en DataFrame Spark au format
 * {@link GameFeatureBuilder#TEAM_GAME_SCHEMA} : une ligne par équipe et par match.
 */
@Component
@ConditionalOnSpark
@RequiredArgsConstructor
class TeamGameLoader {

    private final GameHistory history;
    private final SparkSession spark;

    /** Les {@code count} saisons les plus récentes qui ont des matchs joués, de la plus ancienne à la plus récente. */
    List<Integer> recentSeasonYears(int count) {
        return history.recentSeasonYears(count);
    }

    Dataset<Row> teamGames(List<Integer> seasonYears) {
        List<Row> rows = history.games(seasonYears).stream()
                .flatMap(game -> Stream.of(teamRow(game, true), teamRow(game, false)))
                .toList();
        return spark.createDataFrame(rows, GameFeatureBuilder.TEAM_GAME_SCHEMA);
    }

    static Row teamRow(GameRecord game, boolean home) {
        Integer scored = home ? game.homePoints() : game.awayPoints();
        Integer allowed = home ? game.awayPoints() : game.homePoints();
        BoxScore own = home ? game.home() : game.away();
        BoxScore opponent = home ? game.away() : game.home();
        boolean bothBoxScores = own != null && opponent != null;
        return RowFactory.create(
                game.gameId(),
                game.season(),
                game.seasonYear(),
                Date.valueOf(game.date()),
                home ? game.homeTeamId() : game.awayTeamId(),
                home ? 1 : 0,
                game.playoff() ? 1 : 0,
                game.played() ? (scored > allowed ? 1 : 0) : null,
                scored,
                allowed,
                bothBoxScores ? own.possessions() : null,
                bothBoxScores ? opponent.possessions() : null);
    }
}
