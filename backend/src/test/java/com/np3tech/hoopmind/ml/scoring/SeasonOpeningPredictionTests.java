package com.np3tech.hoopmind.ml.scoring;

import com.np3tech.hoopmind.ml.GameOutcomeAnalysis;
import com.np3tech.hoopmind.ml.MlProperties;
import com.np3tech.hoopmind.ml.PredictionFactor;
import com.np3tech.hoopmind.nba.GameHistory;
import com.np3tech.hoopmind.nba.GameRecord;
import com.np3tech.hoopmind.nba.GameRecord.BoxScore;
import com.np3tech.hoopmind.shared.exception.AppException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Le soir d'ouverture, aucune équipe n'a encore joué : la forme part du bilan de la saison <b>précédente</b>
 * (ADR 0022). L'historique simulé ne renvoie des matchs que pour 2023 : une prédiction en 2024 doit les lire.
 */
class SeasonOpeningPredictionTests {

    private static final long CELTICS = 1;
    private static final long KNICKS = 2;
    private static final long EXPANSION = 3;
    private static final LocalDate OPENING_NIGHT = LocalDate.of(2024, 10, 22);
    private static final BoxScore BOX = new BoxScore(90, 10, 20, 0);

    @TempDir
    Path modelDir;

    @Test
    void onOpeningNightTheFormComesFromLastSeason() {
        GameOutcomeAnalysis analysis = predictor().analyze(10, CELTICS, KNICKS, OPENING_NIGHT, false);

        // Saison passée : Boston 3 victoires sur 4, New York 1 sur 4.
        assertThat(analysis.homeGamesPlayed()).isZero();
        assertThat(factor(analysis, "win_pct_diff").homeValue()).isCloseTo(0.75, within(1e-9));
        assertThat(factor(analysis, "win_pct_diff").awayValue()).isCloseTo(0.25, within(1e-9));
    }

    @Test
    void aTeamWithoutAnyGameThisSeasonOrLastIsRefused() {
        assertThatThrownBy(() -> predictor().analyze(10, CELTICS, EXPANSION, OPENING_NIGHT, false))
                .isInstanceOfSatisfying(AppException.class, e -> assertThat(e.getErrorCode()).isEqualTo("NO_TEAM_FORM"));
    }

    private ScoringGameOutcomePredictor predictor() {
        ScoringCardStore cards = new ScoringCardStore(new MlProperties(5, modelDir.toString()), JsonMapper.builder().build());
        cards.save(new ScoringCard("logistic-regression", "v1", GameOutcomeFeatures.NAMES,
                new double[]{0, 0, 0, 0, 0}, new double[]{1, 1, 1, 1, 1}, new double[]{1, 0, 0, 0, 0}, 0.1));
        return new ScoringGameOutcomePredictor(new LastSeasonOnly(), cards, new SimpleMeterRegistry());
    }

    private static PredictionFactor factor(GameOutcomeAnalysis analysis, String name) {
        return analysis.factors().stream().filter(f -> f.feature().equals(name)).findFirst().orElseThrow();
    }

    /** Des matchs pour 2023-24 seulement ; la saison 2024-25 n'a encore rien de joué. */
    private static final class LastSeasonOnly implements GameHistory {

        @Override
        public List<GameRecord> teamGames(int seasonYear, Collection<Long> teamIds) {
            if (seasonYear != 2023) {
                return List.of();
            }
            LocalDate day = LocalDate.of(2024, 1, 10);
            return List.of(
                    new GameRecord(1, "2023-24", 2023, day, CELTICS, KNICKS, false, 110, 100, BOX, BOX),
                    new GameRecord(2, "2023-24", 2023, day.plusDays(2), KNICKS, CELTICS, false, 105, 101, BOX, BOX),
                    new GameRecord(3, "2023-24", 2023, day.plusDays(4), CELTICS, KNICKS, false, 120, 99, BOX, BOX),
                    new GameRecord(4, "2023-24", 2023, day.plusDays(6), KNICKS, CELTICS, false, 98, 112, BOX, BOX));
        }

        @Override
        public Optional<Integer> seasonYear(long seasonId) {
            return Optional.of(2024);
        }

        @Override
        public List<Integer> recentSeasonYears(int count) {
            return List.of(2023);
        }

        @Override
        public List<GameRecord> games(Collection<Integer> seasonYears) {
            return List.of();
        }

        @Override
        public Map<Long, String> teamNames(Collection<Long> teamIds) {
            return Map.of();
        }
    }
}
