package com.np3tech.hoopmind.ml.spark;

import com.np3tech.hoopmind.ml.GameOutcomeModel;
import com.np3tech.hoopmind.ml.MlProperties;
import com.np3tech.hoopmind.ml.TrainingReport;
import com.np3tech.hoopmind.ml.entity.PredictionModelMetrics;
import com.np3tech.hoopmind.ml.repository.PredictionModelMetricsRepository;
import com.np3tech.hoopmind.ml.scoring.ScoringCardStore;
import com.np3tech.hoopmind.ml.spark.GameOutcomeTrainer.Evaluation;
import com.np3tech.hoopmind.shared.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.apache.spark.sql.functions.col;

/**
 * Entraînement et suivi du modèle de prédiction des matchs, à partir des données importées. Spark ne sert qu'à
 * entraîner : le modèle est enregistré sous forme de carte de score, appliquée en Java pour chaque prédiction.
 * Chaque entraînement produit deux modèles de même version : le vainqueur (régression logistique) et le score (deux
 * régressions linéaires), chacun avec sa ligne de métriques.
 */
@Service
@ConditionalOnSpark
@RequiredArgsConstructor
@Slf4j
public class GameOutcomeModelService implements GameOutcomeModel {

    public static final String MODEL_NAME = "game-outcome-logistic-regression";
    public static final String SCORE_MODEL_NAME = "game-score-linear-regression";

    private final TeamGameLoader loader;
    private final PredictionModelMetricsRepository metricsRepository;
    private final ScoringCardStore scoringCards;
    private final MlProperties properties;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    /**
     * @param seasons nombre de saisons (les plus récentes) à utiliser ; {@code null} reprend {@code hoopmind.ml.training-seasons}
     */
    @Override
    public TrainingReport train(Integer seasons) {
        int seasonCount = seasons != null ? seasons : properties.trainingSeasons();
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
        GameScoreTrainer.Result score;
        try {
            // La saison qui précède la première sert seulement de point de départ à la forme (bilan de la saison passée).
            List<Integer> loaded = Stream.concat(Stream.of(years.getFirst() - 1), years.stream()).toList();
            Dataset<Row> features = GameFeatureBuilder.gameFeatures(loader.teamGames(loaded))
                    .filter(col("season_year").isin(years.toArray()));
            result = GameOutcomeTrainer.train(features);
            score = GameScoreTrainer.train(features);
        } catch (IllegalStateException e) {
            throw new AppException(e.getMessage(), HttpStatus.UNPROCESSABLE_CONTENT, "NO_TRAINING_DATA");
        }
        String version = LocalDateTime.now(clock).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        scoringCards.save(GameOutcomeTrainer.scoringCard(result.model(), MODEL_NAME, version));
        scoringCards.save(GameScoreTrainer.scoreCard(score.model(), SCORE_MODEL_NAME, version,
                score.evaluation().marginMeanAbsoluteError()));

        PredictionModelMetrics metrics = metricsRepository.save(toMetrics(result.evaluation(), years.size(), version));
        PredictionModelMetrics scoreMetrics =
                metricsRepository.save(toScoreMetrics(score.evaluation(), years.size(), version));
        TrainingReport report = toReport(metrics, result.evaluation().trainingGames(), Optional.of(scoreMetrics));
        log.info("Modèle entraîné : précision {} (référence meilleur bilan {}, domicile {}) ; score : erreur moyenne "
                        + "sur l'écart {} points (règle simple {})", report.accuracy(), report.betterRecordBaseline(),
                report.homeTeamBaseline(), report.score().marginMeanAbsoluteError(),
                report.score().baselineMarginMeanAbsoluteError());
        return report;
    }

    /** Dernier entraînement enregistré. */
    @Override
    public Optional<TrainingReport> latestReport() {
        return metricsRepository.findFirstByModelOrderByCreatedAtDesc(MODEL_NAME).map(m -> toReport(m, null,
                metricsRepository.findFirstByModelOrderByCreatedAtDesc(SCORE_MODEL_NAME)
                        .filter(score -> score.getModelVersion().equals(m.getModelVersion()))));
    }

    private PredictionModelMetrics toMetrics(Evaluation e, int seasonsUsed, String version) {
        return PredictionModelMetrics.builder()
                .model(MODEL_NAME)
                .modelVersion(version)
                .evaluationDate(LocalDate.now(clock))
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

    private PredictionModelMetrics toScoreMetrics(GameScoreTrainer.Evaluation e, int seasonsUsed, String version) {
        return PredictionModelMetrics.builder()
                .model(SCORE_MODEL_NAME)
                .modelVersion(version)
                .evaluationDate(LocalDate.now(clock))
                .totalPredictions((int) e.testGames())
                .correctPredictions((int) e.correctWinners())
                .accuracy(decimal(e.winnerAccuracy()))
                .meanAbsoluteError(points(e.marginMeanAbsoluteError()))
                .rootMeanSquaredError(points(e.marginRootMeanSquaredError()))
                .pointsMeanAbsoluteError(points(e.pointsMeanAbsoluteError()))
                .totalMeanAbsoluteError(points(e.totalMeanAbsoluteError()))
                .baselineMeanAbsoluteError(points(e.baselineMarginMeanAbsoluteError()))
                .trainingSeasons(seasonsUsed)
                .testSeason(e.testSeason().length() > 20 ? e.testSeason().substring(0, 20) : e.testSeason())
                .build();
    }

    /** @param score évaluation du modèle de score de la même version, s'il a été entraîné avec */
    private TrainingReport toReport(PredictionModelMetrics m, Long trainingGames, Optional<PredictionModelMetrics> score) {
        return new TrainingReport(m.getModel(), m.getModelVersion(), m.getCreatedAt(), m.getTrainingSeasons(),
                m.getTestSeason(), trainingGames, m.getTotalPredictions(), m.getAccuracy().doubleValue(),
                number(m.getAuc()), number(m.getLogLoss()), number(m.getPrecisionScore()), number(m.getRecall()),
                number(m.getF1Score()), number(m.getHomeBaselineAccuracy()), number(m.getBaselineAccuracy()),
                read(m.getConfusionMatrixJson(), new TypeReference<>() {
                }),
                read(m.getFeatureImportanceJson(), new TypeReference<>() {
                }),
                score.map(GameOutcomeModelService::toScoreReport).orElse(null));
    }

    private static TrainingReport.ScoreReport toScoreReport(PredictionModelMetrics s) {
        return new TrainingReport.ScoreReport(s.getModel(), s.getTotalPredictions(), s.getAccuracy().doubleValue(),
                number(s.getPointsMeanAbsoluteError()), s.getMeanAbsoluteError().doubleValue(),
                number(s.getRootMeanSquaredError()), number(s.getTotalMeanAbsoluteError()),
                number(s.getBaselineMeanAbsoluteError()));
    }

    private <T> T read(String json, TypeReference<T> type) {
        return json == null ? null : jsonMapper.readValue(json, type);
    }

    private static BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    private static BigDecimal points(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static Double number(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }
}
