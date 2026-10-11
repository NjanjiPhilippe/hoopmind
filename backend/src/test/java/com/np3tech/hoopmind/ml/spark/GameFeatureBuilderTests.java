package com.np3tech.hoopmind.ml.spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.np3tech.hoopmind.ml.spark.SparkTestSession.teamGame;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GameFeatureBuilderTests {

    private static final long CELTICS = 1;
    private static final long LAKERS = 2;
    private static final LocalDate DAY = LocalDate.of(2024, 10, 22);

    /** Trois matchs Celtics-Lakers : victoire des Celtics, victoire des Lakers, puis un match à venir. */
    private final Dataset<Row> teamGames = SparkTestSession.get().createDataFrame(List.of(
            teamGame(1, "2024-2025", DAY, CELTICS, true, 120, 100),
            teamGame(1, "2024-2025", DAY, LAKERS, false, 100, 120),
            teamGame(2, "2024-2025", DAY.plusDays(2), LAKERS, true, 110, 105),
            teamGame(2, "2024-2025", DAY.plusDays(2), CELTICS, false, 105, 110),
            teamGame(3, "2024-2025", DAY.plusDays(10), CELTICS, true, null, null),
            teamGame(3, "2024-2025", DAY.plusDays(10), LAKERS, false, null, null)
    ), GameFeatureBuilder.TEAM_GAME_SCHEMA);

    @Test
    void formOnlyUsesGamesPlayedBeforeTheMatch() {
        Dataset<Row> games = GameFeatureBuilder.gameFeatures(teamGames).orderBy("game_id");
        List<Row> rows = games.collectAsList();

        // Premier match : aucune donnée antérieure, valeurs neutres (et surtout pas le résultat du match lui-même).
        Row first = rows.get(0);
        assertThat(first.<Long>getAs("home_games_played")).isZero();
        assertThat(first.<Double>getAs("home_win_pct")).isEqualTo(0.5);
        assertThat(first.<Double>getAs("home_won")).isEqualTo(1.0);

        // Deuxième match : les Lakers (à domicile) n'ont qu'une défaite derrière eux.
        Row second = rows.get(1);
        assertThat(second.<Long>getAs("home_team_id")).isEqualTo(LAKERS);
        assertThat(second.<Double>getAs("home_win_pct")).isZero();
        assertThat(second.<Double>getAs("away_win_pct")).isEqualTo(1.0);
        assertThat(second.<Double>getAs("home_net_rating")).isCloseTo(-20.0, within(1e-9));
        assertThat(second.<Double>getAs("rest_days_diff")).isZero();

        // Match à venir : bilan 1-1 pour les deux équipes, aucun résultat connu.
        Row upcoming = rows.get(2);
        assertThat(upcoming.<Long>getAs("home_games_played")).isEqualTo(2);
        assertThat(upcoming.<Double>getAs("home_win_pct")).isEqualTo(0.5);
        assertThat(upcoming.<Double>getAs("home_offensive_rating")).isCloseTo(112.5, within(1e-9));
        assertThat(upcoming.<Double>getAs("home_rest_days")).isEqualTo(7.0);
        assertThat(upcoming.<Object>getAs("home_won")).isNull();
    }
}
