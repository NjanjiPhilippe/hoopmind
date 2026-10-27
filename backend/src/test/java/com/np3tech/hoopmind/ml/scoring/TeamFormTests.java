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
        // 112,5 sur deux matchs, ramené vers 110 (valeur neutre) qui compte comme 10 matchs.
        assertThat(later.offensiveRating()).isCloseTo((112.5 * 2 + 110 * 10) / 12, within(1e-9));
        assertThat(later.netRating()).isCloseTo(7.5 * 2 / 12, within(1e-9));
        assertThat(later.restDays()).as("dernier match la veille").isEqualTo(1.0);
    }

    @Test
    void lastSeasonFillsTheFormAtTheStartOfTheSeason() {
        // Saison passée : les Celtics gagnent 3 matchs sur 4, à 100 possessions par match.
        List<GameRecord> lastSeason = List.of(
                game(11, DAY.minusYears(1), CELTICS, LAKERS, 110, 100),
                game(12, DAY.minusYears(1).plusDays(2), LAKERS, CELTICS, 100, 120),
                game(13, DAY.minusYears(1).plusDays(4), CELTICS, LAKERS, 100, 90),
                game(14, DAY.minusYears(1).plusDays(6), LAKERS, CELTICS, 110, 90));
        TeamPrior prior = TeamPrior.of(lastSeason, CELTICS);
        assertThat(prior.gamesPlayed()).isEqualTo(4);
        assertThat(prior.winPct()).isEqualTo(0.75);
        assertThat(prior.offensiveRating()).isCloseTo(105.0, within(1e-9));
        assertThat(prior.defensiveRating()).isCloseTo(100.0, within(1e-9));

        TeamForm opening = TeamForm.before(season, CELTICS, DAY, null, prior);
        assertThat(opening.gamesPlayed()).isZero();
        assertThat(opening.winPct()).isEqualTo(0.75);
        assertThat(opening.last10WinPct()).isEqualTo(0.75);
        assertThat(opening.netRating()).isCloseTo(5.0, within(1e-9));

        // Après une victoire et une défaite : (1 + 10 × 0,75) / 12 et (1 + 8 × 0,75) / 10.
        TeamForm later = TeamForm.before(season, CELTICS, DAY.plusDays(3), null, prior);
        assertThat(later.winPct()).isCloseTo(8.5 / 12, within(1e-9));
        assertThat(later.last10WinPct()).isCloseTo(0.7, within(1e-9));
        assertThat(TeamPrior.of(lastSeason, 99)).isEqualTo(TeamPrior.NONE);
    }

    @Test
    void pointsPerGameBlendThisSeasonWithLastSeasonAverage() {
        // Saison passée : les Celtics marquent 105 points et en encaissent 95 par match.
        List<GameRecord> lastSeason = List.of(
                game(11, DAY.minusYears(1), CELTICS, LAKERS, 110, 100),
                game(12, DAY.minusYears(1).plusDays(2), LAKERS, CELTICS, 90, 100));
        TeamPrior prior = TeamPrior.of(lastSeason, CELTICS);
        assertThat(prior.pointsPerGame()).isEqualTo(105.0);
        assertThat(prior.pointsAllowedPerGame()).isEqualTo(95.0);

        TeamForm opening = TeamForm.before(season, CELTICS, DAY, null, prior);
        assertThat(opening.pointsPerGame()).isEqualTo(105.0);
        assertThat(opening.pointsAllowedPerGame()).isEqualTo(95.0);

        // 225 points marqués et 210 encaissés en deux matchs, plus la saison passée qui compte comme 10 matchs ; le match
        // à venir (sans score) ne compte pas.
        TeamForm later = TeamForm.before(season, CELTICS, DAY.plusDays(20), null, prior);
        assertThat(later.pointsPerGame()).isCloseTo((225 + 10 * 105.0) / 12, within(1e-9));
        assertThat(later.pointsAllowedPerGame()).isCloseTo((210 + 10 * 95.0) / 12, within(1e-9));
        assertThat(TeamForm.before(season, CELTICS, DAY, null).pointsPerGame()).isEqualTo(TeamForm.NEUTRAL_POINTS);
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
