package com.np3tech.hoopmind.ml.training;

import com.np3tech.hoopmind.IntegrationTest;
import com.np3tech.hoopmind.TestAccounts;
import com.np3tech.hoopmind.ml.GameOutcomeModel;
import com.np3tech.hoopmind.ml.ModelTrainingEvent;
import com.np3tech.hoopmind.ml.TrainingReport;
import com.np3tech.hoopmind.ml.training.ModelTrainingJobs.Phase;
import com.np3tech.hoopmind.shared.job.JobService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class ModelTrainingJobsTests {

    @Autowired
    JobService jobService;

    @Autowired
    MockMvcTester mvc;

    @Test
    void trainsInTheBackgroundAndPublishesTheResult() throws Exception {
        TrainingReport report = new TrainingReport("game-outcome", "v-test", LocalDateTime.of(2026, 10, 11, 12, 0), 3,
                "2024-25", 2_000L, 1_200, 0.66, 0.71, 0.62, 0.65, 0.7, 0.67, 0.58, 0.63, Map.of("TP", 10L),
                Map.of("win_pct_diff", 1.2));
        GameOutcomeModel engine = new GameOutcomeModel() {
            @Override
            public TrainingReport train(Integer seasons) {
                return report;
            }

            @Override
            public Optional<TrainingReport> latestReport() {
                return Optional.of(report);
            }
        };
        List<Object> events = new CopyOnWriteArrayList<>();
        ModelTrainingJobs trainings = new ModelTrainingJobs(
                new StaticListableBeanFactory(Map.of("engine", engine)).getBeanProvider(GameOutcomeModel.class),
                jobService, events::add);

        assertThat(trainings.trainAndWait(3)).isEqualTo(report);
        assertThat(trainings.latest()).hasValueSatisfying(status -> {
            assertThat(status.phase()).isEqualTo(Phase.SUCCEEDED);
            assertThat(status.seasons()).isEqualTo(3);
            assertThat(status.report()).isEqualTo(report);
        });
        assertThat(events).containsExactly(new ModelTrainingEvent.ModelTrained(null, "v-test", 0.66, "2024-25"));
    }

    @Test
    void withoutTheSparkEngineTheTrainingIsUnavailableNotMissing() {
        assertThat(mvc.post().uri("/api/v1/spark/ml/train/game-outcome/jobs")
                .header(HttpHeaders.AUTHORIZATION, TestAccounts.admin(mvc).bearer()))
                .hasStatus(503)
                .bodyJson().extractingPath("$.code").isEqualTo("MODEL_ENGINE_DISABLED");
        assertThat(mvc.get().uri("/api/v1/spark/ml/train/game-outcome/status")
                .header(HttpHeaders.AUTHORIZATION, TestAccounts.register(mvc).bearer()))
                .hasStatus(403);
    }

    @Test
    void predictionsDoNotNeedSparkButNeedATrainedModel() {
        assertThat(mvc.get().uri("/api/v1/spark/ml/predict/game")
                .param("seasonId", "1").param("homeTeamId", "1").param("awayTeamId", "2")
                .header(HttpHeaders.AUTHORIZATION, TestAccounts.register(mvc).bearer()))
                .hasStatus(409)
                .bodyJson().extractingPath("$.code").isEqualTo("MODEL_NOT_TRAINED");
    }

    @Test
    void explanationsFollowTheSameRules() {
        assertThat(mvc.get().uri("/api/v1/spark/ml/predict/game/explanation")
                .param("seasonId", "1").param("homeTeamId", "1").param("awayTeamId", "2"))
                .hasStatus(401);
        assertThat(mvc.get().uri("/api/v1/spark/ml/predict/game/explanation")
                .param("seasonId", "1").param("homeTeamId", "1").param("awayTeamId", "2")
                .header(HttpHeaders.AUTHORIZATION, TestAccounts.register(mvc).bearer()))
                .hasStatus(409)
                .bodyJson().extractingPath("$.code").isEqualTo("MODEL_NOT_TRAINED");
    }
}
