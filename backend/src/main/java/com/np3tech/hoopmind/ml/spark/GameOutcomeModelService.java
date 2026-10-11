package com.np3tech.hoopmind.ml.spark;

import com.np3tech.hoopmind.ml.entity.PredictionModelMetrics;
import com.np3tech.hoopmind.ml.repository.PredictionModelMetricsRepository;
import com.np3tech.hoopmind.ml.spark.GameOutcomeTrainer.Evaluation;
import com.np3tech.hoopmind.nba.entity.Season;
import com.np3tech.hoopmind.nba.repository.SeasonRepository;
import com.np3tech.hoopmind.shared.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.linalg.Vector;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.spark.sql.functions.col;

/**
 * Entraînement, suivi et utilisation du modèle de prédiction des matchs, à partir des données importées.
 */
@Service
@ConditionalOnSpark
@RequiredArgsConstructor
@Slf4j
public class GameOutcomeModelService {

    public static final String MODEL_NAME = "game-outcome-logistic-regression";

    private final TeamGameLoader loader;
    private final SeasonRepository seasonRepository;
    private final PredictionModelMetricsRepository metricsRepository;
    private final JsonMapper jsonMapper;

    @Value("${hoopmind.ml.training-seasons:5}")
    private int defaultTrainingSeasons;

    @Value("${hoopmind.ml.model-dir:spark-models}")
    private String modelDirectory;

    private final AtomicReference<PipelineModel> currentModel = new AtomicReference<>();

    /** Résultat d'un entraînement, tel qu'il est montré à l'administrateur. */
    public record TrainingReport(String model, String version, LocalDateTime trainedAt, Integer seasonsUsed,
                                 String testSeason, Long trainingGames, long testGames, double accuracy, Double auc,
                                 Double logLoss, Double precision, Double recall, Double f1, Double homeTeamBaseline,
                                 Double betterRecordBaseline, Map<String, Long> confusionMatrix,
                                 Map<String, Double> featureWeights) {
    }

    /**
     * @param seasons nombre de saisons (les plus récentes) à utiliser ; {@code null} reprend {@code hoopmind.ml.training-seasons}
     */
    public TrainingReport train(Integer seasons) {
        int seasonCount = seasons != null ? seasons : defaultTrainingSeasons;
        if (seasonCount < 1 || seasonCount > 30) {
            throw new AppException("Le nombre de saisons doit être compris entre 1 et 30.", HttpStatus.BAD_REQUEST,
                    "INVALID_TRAINING_SEASONS");
        }
        List<Integer> years = loader.recentSeasonYears(seasonCount);
        if (years.isEmpty()) {
            throw new AppException("Aucun match joué en base : importez d'abord la base NBA.",
                    HttpStatus.UNPROCESSABLE_CONTENT, "NO_TRAINING_DATA");
        }
        log.info("Entraînement du modèle de prédiction sur les saisons {}", years);

        GameOutcomeTrainer.Result result;
        try {
            result = GameOutcomeTrainer.train(GameFeatureBuilder.gameFeatures(loader.teamGames(years)));
        } catch (IllegalStateException e) {
            throw new AppException(e.getMessage(), HttpStatus.UNPROCESSABLE_CONTENT, "NO_TRAINING_DATA");
        }
        save(result.model());
        currentModel.set(result.model());

        PredictionModelMetrics metrics = metricsRepository.save(toMetrics(result.evaluation(), years.size()));
        TrainingReport report = toReport(metrics, result.evaluation().trainingGames());
        log.info("Modèle entraîné : précision {} (référence meilleur bilan {}, domicile {})",
                report.accuracy(), report.betterRecordBaseline(), report.homeTeamBaseline());
        return report;
    }

    /** Dernier entraînement enregistré. */
    public Optional<TrainingReport> latestReport() {
        return metricsRepository.findFirstByModelOrderByCreatedAtDesc(MODEL_NAME).map(m -> toReport(m, null));
    }

    /** Probabilité de victoire de l'équipe à domicile, d'après la forme actuelle des deux équipes dans la saison. */
    public double homeWinProbability(Long seasonId, Long homeTeamId, Long awayTeamId) {
        PipelineModel model = model();
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new AppException("Saison introuvable : " + seasonId, HttpStatus.NOT_FOUND, "SEASON_NOT_FOUND"));
        Dataset<Row> form = GameFeatureBuilder.currentForm(loader.teamGames(List.of(season.getYear())));
        Dataset<Row> home = form.filter(col("team_id").equalTo(homeTeamId));
        Dataset<Row> away = form.filter(col("team_id").equalTo(awayTeamId));
        if (home.isEmpty() || away.isEmpty()) {
            throw new AppException("Ces équipes n'ont pas encore joué de match dans cette saison.",
                    HttpStatus.UNPROCESSABLE_CONTENT, "NO_TEAM_FORM");
        }
        Row prediction = model.transform(GameFeatureBuilder.matchup(home, away, false)).first();
        Vector probability = prediction.getAs("probability");
        return probability.apply(1);
    }

    private PipelineModel model() {
        PipelineModel model = currentModel.get();
        if (model != null) {
            return model;
        }
        Path path = modelPath();
        if (!Files.isDirectory(path)) {
            throw new AppException("Le modèle n'est pas encore entraîné (POST /api/v1/spark/ml/train/game-outcome).",
                    HttpStatus.CONFLICT, "MODEL_NOT_TRAINED");
        }
        model = PipelineModel.load(path.toString());
        currentModel.compareAndSet(null, model);
        return model;
    }

    private void save(PipelineModel model) {
        try {
            model.write().overwrite().save(modelPath().toString());
        } catch (IOException e) {
            throw new AppException("Impossible d'enregistrer le modèle : " + e.getMessage(), e,
                    HttpStatus.INTERNAL_SERVER_ERROR, "MODEL_SAVE_FAILED");
        }
    }

    private Path modelPath() {
        return Path.of(modelDirectory, "game-outcome");
    }

    private PredictionModelMetrics toMetrics(Evaluation e, int seasonsUsed) {
        return PredictionModelMetrics.builder()
                .model(MODEL_NAME)
                .modelVersion(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")))
                .evaluationDate(LocalDate.now())
                .totalPredictions((int) e.testGames())
                .correctPredictions((int) e.correctPredictions())
                .accuracy(decimal(e.accuracy()))
                .precisionScore(decimal(e.precision()))
                .recall(decimal(e.recall()))
                .f1Score(decimal(e.f1()))
                .auc(decimal(e.auc()))
                .logLoss(decimal(e.logLoss()))
                .baselineAccuracy(decimal(e.betterRecordBaseline()))
                .homeBaselineAccuracy(decimal(e.homeTeamBaseline()))
                .trainingSeasons(seasonsUsed)
                .testSeason(e.testSeason().length() > 20 ? e.testSeason().substring(0, 20) : e.testSeason())
                .confusionMatrixJson(jsonMapper.writeValueAsString(e.confusionMatrix()))
                .featureImportanceJson(jsonMapper.writeValueAsString(e.featureWeights()))
                .build();
    }

    private TrainingReport toReport(PredictionModelMetrics m, Long trainingGames) {
        return new TrainingReport(m.getModel(), m.getModelVersion(), m.getCreatedAt(), m.getTrainingSeasons(),
                m.getTestSeason(), trainingGames, m.getTotalPredictions(), m.getAccuracy().doubleValue(),
                number(m.getAuc()), number(m.getLogLoss()), number(m.getPrecisionScore()), number(m.getRecall()),
                number(m.getF1Score()), number(m.getHomeBaselineAccuracy()), number(m.getBaselineAccuracy()),
                read(m.getConfusionMatrixJson(), new TypeReference<>() {
                }),
                read(m.getFeatureImportanceJson(), new TypeReference<>() {
                }));
    }

    private <T> T read(String json, TypeReference<T> type) {
        return json == null ? null : jsonMapper.readValue(json, type);
    }

    private static BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    private static Double number(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }
}
