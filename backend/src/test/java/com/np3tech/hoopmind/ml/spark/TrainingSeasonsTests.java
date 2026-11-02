package com.np3tech.hoopmind.ml.spark;

import com.np3tech.hoopmind.ml.MlProperties;
import com.np3tech.hoopmind.ml.TrainingReport;
import com.np3tech.hoopmind.ml.repository.PredictionModelMetricsRepository;
import com.np3tech.hoopmind.ml.scoring.ScoringCardStore;
import com.np3tech.hoopmind.nba.GameHistory;
import com.np3tech.hoopmind.nba.GameRecord;
import com.np3tech.hoopmind.nba.GameRecord.BoxScore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * L'entraînement charge la saison qui précède la première saison voulue, pour le bilan de départ de chaque équipe
 * (ADR 0022), puis l'écarte : elle ne doit ni manquer (écart avec la prédiction), ni servir à apprendre (fuite).
 */
class TrainingSeasonsTests {

    private static final int GAMES_PER_SEASON = 4 * 10 * 9;

    @TempDir
    Path modelDir;

    @Test
    void loadsTheSeasonBeforeTheFirstOneForItsPriorThenLeavesItOut() {
        SimulatedHistory history = new SimulatedHistory();
        PredictionModelMetricsRepository metrics = Mockito.mock(PredictionModelMetricsRepository.class);
        when(metrics.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        GameOutcomeModelService service = new GameOutcomeModelService(
                new TeamGameLoader(history, SparkTestSession.get()), metrics,
                Mockito.mock(ScoringCardStore.class),
                new MlProperties(2, modelDir.toString()), JsonMapper.builder().build(), Clock.systemUTC());

        TrainingReport report = service.train(2);

        assertThat(history.requested).containsExactly(List.of(2021, 2022, 2023));
        assertThat(report.testSeason()).isEqualTo("2023-2024");
        // Une saison au plus (le pipeline écarte quelques matchs) : la saison de départ en ferait presque deux.
        assertThat(report.trainingGames()).as("seule 2022-23 sert à apprendre, pas la saison de départ")
                .isBetween((long) (GAMES_PER_SEASON * 0.8), (long) GAMES_PER_SEASON);
    }

    /** Trois saisons de 10 équipes ; les deux plus récentes sont demandées. */
    private static final class SimulatedHistory implements GameHistory {

        final List<List<Integer>> requested = new CopyOnWriteArrayList<>();
        private final Random random = new Random(7);
        private final BoxScore box = new BoxScore(100, 10, 20, 0);

        @Override
        public List<Integer> recentSeasonYears(int count) {
            return List.of(2022, 2023);
        }

        @Override
        public List<GameRecord> games(Collection<Integer> seasonYears) {
            requested.add(List.copyOf(seasonYears));
            List<GameRecord> games = new ArrayList<>();
            long gameId = 0;
            for (int year : seasonYears) {
                String season = year + "-" + (year + 1);
                double[] strength = random.doubles(10, -8, 8).toArray();
                LocalDate day = LocalDate.of(year, 10, 20);
                for (int round = 0; round < 4; round++) {
                    for (int home = 0; home < 10; home++) {
                        for (int away = 0; away < 10; away++) {
                            if (home == away) continue;
                            double margin = strength[home] - strength[away] + 3 + random.nextGaussian() * 11;
                            int homePoints = 110 + (int) Math.round(margin / 2);
                            int awayPoints = 110 - (int) Math.round(margin / 2);
                            if (homePoints == awayPoints) homePoints++;
                            gameId++;
                            day = day.plusDays(gameId % 3 == 0 ? 1 : 0);
                            games.add(new GameRecord(gameId, season, year, day, home, away, false, homePoints,
                                    awayPoints, box, box));
                        }
                    }
                }
            }
            return games;
        }

        @Override
        public List<GameRecord> teamGames(int seasonYear, Collection<Long> teamIds) {
            return List.of();
        }

        @Override
        public Optional<Integer> seasonYear(long seasonId) {
            return Optional.empty();
        }

        @Override
        public Map<Long, String> teamNames(Collection<Long> teamIds) {
            return Map.of();
        }
    }
}
