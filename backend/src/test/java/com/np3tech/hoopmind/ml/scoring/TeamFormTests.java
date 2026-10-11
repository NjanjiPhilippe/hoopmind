package com.np3tech.hoopmind.ml.scoring;

import com.np3tech.hoopmind.nba.GameRecord;
import com.np3tech.hoopmind.nba.GameRecord.BoxScore;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TeamFormTests {

    private static final long CELTICS = 1;
    private static final long LAKERS = 2;
    private static final LocalDate DAY = LocalDate.of(2024, 10, 22);
    private static final BoxScore HUNDRED_POSSESSIONS = new BoxScore(90, 10, 20, 0);

    private final List<GameRecord> season = List.of(
            game(1, DAY, CELTICS, LAKERS, 120, 100),
            game(2, DAY.plusDays(2), LAKERS, CELTICS, 110, 105),
            game(3, DAY.plusDays(10), CELTICS, LAKERS, null, null));

    @Test
    void onlyCountsGamesBeforeTheDate() {
        TeamForm opening = TeamForm.before(season, CELTICS, DAY, null);
        assertThat(opening.gamesPlayed()).isZero();
        assertThat(opening.winPct()).isEqualTo(0.5);
        assertThat(opening.restDays()).isEqualTo(7.0);

        TeamForm later = TeamForm.before(season, CELTICS, DAY.plusDays(3), null);
        assertThat(later.gamesPlayed()).isEqualTo(2);
        assertThat(later.winPct()).isEqualTo(0.5);
        assertThat(later.offensiveRating()).isCloseTo(112.5, within(1e-9));
        assertThat(later.netRating()).isCloseTo(7.5, within(1e-9));
        assertThat(later.restDays()).as("dernier match la veille").isEqualTo(1.0);
    }

    @Test
    void restDaysAreCappedAndPlayoffIsAFeature() {
        TeamForm celtics = TeamForm.before(season, CELTICS, DAY.plusDays(30), null);
        TeamForm lakers = TeamForm.before(season, LAKERS, DAY.plusDays(30), null);
        assertThat(celtics.restDays()).isEqualTo(7.0);

        double[] features = GameOutcomeFeatures.of(celtics, lakers, true);
        assertThat(features).hasSize(GameOutcomeFeatures.NAMES.size());
        assertThat(features[4]).isEqualTo(1.0);
    }

    private static GameRecord game(long id, LocalDate date, long home, long away, Integer homePoints, Integer awayPoints) {
        BoxScore box = homePoints == null ? null : HUNDRED_POSSESSIONS;
        return new GameRecord(id, "2024-25", 2024, date, home, away, false, homePoints, awayPoints, box, box);
    }
}
