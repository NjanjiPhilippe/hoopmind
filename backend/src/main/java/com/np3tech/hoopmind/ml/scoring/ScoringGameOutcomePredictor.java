package com.np3tech.hoopmind.ml.scoring;

import com.np3tech.hoopmind.ml.GameOutcomeAnalysis;
import com.np3tech.hoopmind.ml.GameOutcomePrediction;
import com.np3tech.hoopmind.ml.GameOutcomePredictor;
import com.np3tech.hoopmind.ml.PredictionFactor;
import com.np3tech.hoopmind.nba.GameHistory;
import com.np3tech.hoopmind.nba.GameRecord;
import com.np3tech.hoopmind.shared.exception.AppException;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Prédit un match en Java pur, en quelques millisecondes : forme des deux équipes à la date du match (même calcul
 * qu'à l'entraînement), puis la régression logistique entraînée par Spark, appliquée avec sa carte de score.
 * Spark n'est nécessaire que pour entraîner ; les prédictions fonctionnent même s'il est désactivé.
 * <p>
 * Monitoring ({@code /actuator/prometheus}) : {@code hoopmind.ml.predictions} mesure chaque prédiction (par version
 * du modèle et résultat, {@code ok} ou le code d'erreur), et {@code hoopmind.ml.home.win.probability} répartit les
 * probabilités prédites. Une répartition qui se déplace sans nouvel entraînement signale une dérive des données.
 */
@Component
class ScoringGameOutcomePredictor implements GameOutcomePredictor {

    private final GameHistory history;
    private final ScoringCardStore cards;
    private final MeterRegistry registry;

    ScoringGameOutcomePredictor(GameHistory history, ScoringCardStore cards, MeterRegistry registry) {
        this.history = history;
        this.cards = cards;
        this.registry = registry;
    }

    @Override
    public GameOutcomeAnalysis analyze(long seasonId, long homeTeamId, long awayTeamId, LocalDate date, boolean playoff) {
        Timer.Sample sample = Timer.start(registry);
        String version = "none";
        String outcome = "ok";
        try {
            GameOutcomeAnalysis analysis = score(seasonId, homeTeamId, awayTeamId, date, playoff);
            version = analysis.modelVersion();
            DistributionSummary.builder("hoopmind.ml.home.win.probability")
                    .description("Probabilité de victoire à domicile prédite")
                    .tag("model_version", version)
                    .serviceLevelObjectives(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0)
                    .register(registry).record(analysis.prediction().homeWinProbability());
            return analysis;
        } catch (AppException e) {
            outcome = e.getErrorCode();
            throw e;
        } catch (RuntimeException e) {
            outcome = "error";
            throw e;
        } finally {
            sample.stop(Timer.builder("hoopmind.ml.predictions")
                    .description("Prédictions de match calculées")
                    .tag("model_version", version)
                    .tag("outcome", outcome)
                    .publishPercentileHistogram()
                    .register(registry));
        }
    }

    private GameOutcomeAnalysis score(long seasonId, long homeTeamId, long awayTeamId, LocalDate date, boolean playoff) {
        ScoringCard card = cards.current().orElseThrow(() -> new AppException(
                "Le modèle n'est pas encore entraîné (POST /api/v1/spark/ml/train/game-outcome/jobs).",
                HttpStatus.CONFLICT, "MODEL_NOT_TRAINED"));
        int seasonYear = history.seasonYear(seasonId).orElseThrow(() -> new AppException(
                "Saison introuvable : " + seasonId, HttpStatus.NOT_FOUND, "SEASON_NOT_FOUND"));
        List<GameRecord> games = history.teamGames(seasonYear, List.of(homeTeamId, awayTeamId));

        TeamForm home = TeamForm.before(games, homeTeamId, date, null);
        TeamForm away = TeamForm.before(games, awayTeamId, date, null);
        if (home.gamesPlayed() == 0 || away.gamesPlayed() == 0) {
            throw new AppException("Ces équipes n'ont pas encore joué de match dans cette saison avant cette date.",
                    HttpStatus.UNPROCESSABLE_CONTENT, "NO_TEAM_FORM");
        }
        double[] features = GameOutcomeFeatures.of(home, away, playoff);
        double probability = card.homeWinProbability(features);
        GameOutcomePrediction prediction =
                new GameOutcomePrediction(probability >= 0.5, probability, 1 - probability, card.model());
        return new GameOutcomeAnalysis(prediction, card.version(), card.intercept(),
                factors(home, away, playoff, card.contributions(features)));
    }

    /** Valeurs de chaque côté, dans l'ordre de {@link GameOutcomeFeatures#NAMES}. */
    private static List<PredictionFactor> factors(TeamForm home, TeamForm away, boolean playoff, double[] contributions) {
        double[][] sides = {
                {home.winPct(), away.winPct()},
                {home.last10WinPct(), away.last10WinPct()},
                {home.netRating(), away.netRating()},
                {home.restDays(), away.restDays()},
                {playoff ? 1 : 0, playoff ? 1 : 0}
        };
        List<PredictionFactor> factors = new ArrayList<>();
        for (int i = 0; i < GameOutcomeFeatures.NAMES.size(); i++) {
            factors.add(new PredictionFactor(GameOutcomeFeatures.NAMES.get(i), sides[i][0], sides[i][1], contributions[i]));
        }
        return factors;
    }
}
