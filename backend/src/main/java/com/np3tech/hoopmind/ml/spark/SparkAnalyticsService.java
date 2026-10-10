package com.np3tech.hoopmind.ml.spark;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.spark.sql.functions.*;

@Service
@ConditionalOnSpark
@RequiredArgsConstructor
@Slf4j
public class SparkAnalyticsService {

    private final SparkSession sparkSession;
    private final SparkDataPreparationService dataPreparationService;

    /**
     * Analyse les tendances des équipes sur une saison
     *
     * ANALYSE:
     * - Performance moyenne par équipe
     * - Évolution de la forme au cours de la saison
     * - Comparaison domicile vs extérieur
     *
     * CONCEPTS SPARK:
     * - groupBy(): Regroupe par équipe
     * - agg(): Calcule des statistiques agrégées
     * - avg(), max(), min(): Fonctions d'agrégation
     */
    public Map<String, Object> analyzeTeamTrends(Long seasonId) {
        log.info("Analyzing team trends for season: {}", seasonId);

        Dataset<Row> gameData = dataPreparationService.prepareGameDataset(seasonId);

        // ANALYSE ÉQUIPE DOMICILE
        // Regroupe tous les matchs par équipe domicile et calcule les moyennes
        Dataset<Row> homeAnalysis = gameData.groupBy("home_team_id")
                .agg(
                        count("*").alias("games_played"),
                        sum(when(col("home_team_won").equalTo(1), 1).otherwise(0)).alias("wins"),
                        avg("home_ppg").alias("avg_points_scored"),
                        avg("home_oppg").alias("avg_points_allowed"),
                        avg("home_win_pct").alias("avg_win_percentage"),
                        avg("home_net_rating").alias("avg_net_rating")
                )
                .orderBy(desc("wins"));

        // ANALYSE ÉQUIPE EXTÉRIEURE
        Dataset<Row> awayAnalysis = gameData.groupBy("away_team_id")
                .agg(
                        count("*").alias("games_played"),
                        sum(when(col("home_team_won").equalTo(0), 1).otherwise(0)).alias("wins"),
                        avg("away_ppg").alias("avg_points_scored"),
                        avg("away_oppg").alias("avg_points_allowed"),
                        avg("away_win_pct").alias("avg_win_percentage"),
                        avg("away_net_rating").alias("avg_net_rating")
                )
                .orderBy(desc("wins"));

        Map<String, Object> trends = new HashMap<>();
        trends.put("homeTeamStats", convertToMap(homeAnalysis));
        trends.put("awayTeamStats", convertToMap(awayAnalysis));

        // AVANTAGE DU TERRAIN
        // Calcule si jouer à domicile donne vraiment un avantage
        double homeWinRate = gameData.filter(col("home_team_won").equalTo(1))
                .count() / (double) gameData.count();

        trends.put("homeCourtAdvantage", homeWinRate);
        log.info("Home court advantage: {}%", homeWinRate * 100);

        return trends;
    }

    /**
     * Analyse les corrélations entre statistiques
     *
     * CORRÉLATION:
     * Mesure la relation entre deux variables (-1 à 1)
     * - 1: Corrélation positive parfaite (augmentent ensemble)
     * - 0: Pas de corrélation
     * - -1: Corrélation négative parfaite (inverse)
     *
     * UTILITÉ:
     * - Identifier quelles stats influencent le plus les victoires
     * - Détecter les redondances dans les features
     * - Comprendre les relations entre variables
     *
     * EXEMPLE:
     * Si net_rating a une corrélation de 0.85 avec les victoires,
     * c'est une stat très importante pour prédire le résultat
     */
    public Map<String, Double> analyzeCorrelations(Long seasonId) {
        log.info("Analyzing correlations for season: {}", seasonId);

        Dataset<Row> data = dataPreparationService.prepareGameDataset(seasonId);

        // Liste des variables à analyser
        String[] variables = new String[]{
                "win_pct_diff", "net_rating_diff", "off_rating_diff",
                "def_rating_diff", "ppg_diff", "pace_diff"
        };

        Map<String, Double> correlations = new HashMap<>();

        // Calculer la corrélation de chaque variable avec le résultat
        for (String var : variables) {
            // corr() calcule le coefficient de corrélation de Pearson
            double correlation = data.stat().corr(var, "home_team_won");
            correlations.put(var, correlation);
            log.info("Correlation {} with wins: {}", var, correlation);
        }

        return correlations;
    }

    /**
     * Analyse des performances par période (début/milieu/fin de saison)
     *
     * FENÊTRE GLISSANTE (Window Function):
     * Permet de calculer des statistiques sur un sous-ensemble de données
     *
     * EXEMPLE:
     * - Moyenne mobile sur 10 derniers matchs
     * - Performance sur le dernier mois
     * - Tendance récente vs performance globale
     *
     * UTILITÉ:
     * - Détecter la forme actuelle d'une équipe
     * - Identifier les équipes en progression/régression
     * - Ajuster les prédictions selon la forme récente
     */
    public Map<String, Object> analyzePerformanceByPeriod(Long seasonId) {
        log.info("Analyzing performance by period for season: {}", seasonId);

        Dataset<Row> data = dataPreparationService.prepareGameDataset(seasonId);

        // Ajouter un numéro de match séquentiel
        // row_number() over (partition by team order by date)
        // donne un numéro croissant pour chaque match d'une équipe

        // Diviser la saison en 3 périodes
        long totalGames = data.count();
        long gamesPerPeriod = totalGames / 3;

        // DÉBUT DE SAISON (premier tiers)
        Dataset<Row> earlySeasonStats = data.limit((int) gamesPerPeriod)
                .agg(
                        avg("home_ppg").alias("avg_home_ppg_early"),
                        avg("away_ppg").alias("avg_away_ppg_early"),
                        sum(when(col("home_team_won").equalTo(1), 1).otherwise(0))
                                .divide(gamesPerPeriod)
                                .alias("home_win_rate_early")
                );

        // MILIEU DE SAISON (deuxième tiers)
        Dataset<Row> midSeasonStats = data
                .except(data.limit((int) gamesPerPeriod))
                .limit((int) gamesPerPeriod)
                .agg(
                        avg("home_ppg").alias("avg_home_ppg_mid"),
                        avg("away_ppg").alias("avg_away_ppg_mid"),
                        sum(when(col("home_team_won").equalTo(1), 1).otherwise(0))
                                .divide(gamesPerPeriod)
                                .alias("home_win_rate_mid")
                );

        // FIN DE SAISON (dernier tiers)
        Dataset<Row> lateSeasonStats = data
                .except(data.limit((int) (gamesPerPeriod * 2)))
                .agg(
                        avg("home_ppg").alias("avg_home_ppg_late"),
                        avg("away_ppg").alias("avg_away_ppg_late"),
                        sum(when(col("home_team_won").equalTo(1), 1).otherwise(0))
                                .divide(lit(totalGames - (gamesPerPeriod * 2)))
                                .alias("home_win_rate_late")
                );

        Map<String, Object> periodAnalysis = new HashMap<>();
        periodAnalysis.put("earlySeason", convertToMap(earlySeasonStats).get(0));
        periodAnalysis.put("midSeason", convertToMap(midSeasonStats).get(0));
        periodAnalysis.put("lateSeason", convertToMap(lateSeasonStats).get(0));

        return periodAnalysis;
    }

    /**
     * Analyse l'impact des blessures sur les performances
     *
     * JOINTURE SPARK:
     * Combine des données de plusieurs sources
     * - inner join: Garde seulement les correspondances
     * - left join: Garde toutes les lignes de gauche
     * - outer join: Garde toutes les lignes des deux côtés
     *
     * ANALYSE:
     * Compare les performances des équipes avec/sans joueurs clés blessés
     */
    public Map<String, Object> analyzeInjuryImpact(Long seasonId) {
        log.info("Analyzing injury impact for season: {}", seasonId);

        Dataset<Row> gameData = dataPreparationService.prepareGameDataset(seasonId);

        // TODO: Joindre avec les données de blessures
        // gameData.join(injuryData, "team_id")

        // Comparer performance avec/sans blessures majeures
        // Exemple simplifié:
        Dataset<Row> withoutInjuries = gameData.filter(col("key_player_injured").equalTo(false));
        Dataset<Row> withInjuries = gameData.filter(col("key_player_injured").equalTo(true));

        double winRateHealthy = withoutInjuries.filter(col("home_team_won").equalTo(1))
                .count() / (double) withoutInjuries.count();

        double winRateInjured = withInjuries.filter(col("home_team_won").equalTo(1))
                .count() / (double) withInjuries.count();

        Map<String, Object> injuryImpact = new HashMap<>();
        injuryImpact.put("winRateHealthy", winRateHealthy);
        injuryImpact.put("winRateInjured", winRateInjured);
        injuryImpact.put("impactPercentage", (winRateHealthy - winRateInjured) * 100);

        return injuryImpact;
    }

    /**
     * Analyse des matchups historiques entre équipes
     *
     * HEAD-TO-HEAD ANALYSIS:
     * - Historique des confrontations
     * - Tendances de performance
     * - Avantages spécifiques
     *
     * SQL SPARK:
     * Utilise spark.sql() pour exécuter du SQL pur
     */
    public Map<String, Object> analyzeHeadToHead(Long team1Id, Long team2Id, Long seasonId) {
        log.info("Analyzing head-to-head: Team {} vs Team {}", team1Id, team2Id);

        Dataset<Row> gameData = dataPreparationService.prepareGameDataset(seasonId);

        // Filtrer les matchs entre ces deux équipes
        Dataset<Row> h2hGames = gameData.filter(
                (col("home_team_id").equalTo(team1Id).and(col("away_team_id").equalTo(team2Id)))
                        .or(col("home_team_id").equalTo(team2Id).and(col("away_team_id").equalTo(team1Id)))
        );

        // Statistiques du head-to-head
        long totalGames = h2hGames.count();

        long team1Wins = h2hGames.filter(
                (col("home_team_id").equalTo(team1Id).and(col("home_team_won").equalTo(1)))
                        .or(col("away_team_id").equalTo(team1Id).and(col("home_team_won").equalTo(0)))
        ).count();

        long team2Wins = totalGames - team1Wins;

        // Score moyen
        Row avgScores = h2hGames.agg(
                avg(when(col("home_team_id").equalTo(team1Id), col("home_ppg"))
                        .otherwise(col("away_ppg"))).alias("team1_avg_score"),
                avg(when(col("home_team_id").equalTo(team2Id), col("home_ppg"))
                        .otherwise(col("away_ppg"))).alias("team2_avg_score")
        ).first();

        Map<String, Object> h2hAnalysis = new HashMap<>();
        h2hAnalysis.put("totalGames", totalGames);
        h2hAnalysis.put("team1Wins", team1Wins);
        h2hAnalysis.put("team2Wins", team2Wins);
        h2hAnalysis.put("team1AvgScore", avgScores.getDouble(0));
        h2hAnalysis.put("team2AvgScore", avgScores.getDouble(1));
        h2hAnalysis.put("team1WinRate", team1Wins / (double) totalGames);

        return h2hAnalysis;
    }

    /**
     * Analyse l'évolution de la performance sur la saison
     *
     * ROLLING STATISTICS:
     * Calculs sur une fenêtre glissante de N matchs
     *
     * EXEMPLE:
     * - Moyenne mobile sur 5 derniers matchs
     * - Forme récente vs historique
     * - Détection de tendances
     */
    public Map<String, Object> analyzeTeamMomentum(Long teamId, Long seasonId) {
        log.info("Analyzing momentum for team: {}", teamId);

        Dataset<Row> gameData = dataPreparationService.prepareGameDataset(seasonId);

        // Filtrer les matchs de cette équipe
        Dataset<Row> teamGames = gameData.filter(
                col("home_team_id").equalTo(teamId)
                        .or(col("away_team_id").equalTo(teamId))
        );

        // TODO: Implémenter rolling window pour calculer:
        // - Moyenne mobile des 5 derniers matchs
        // - Série actuelle (winning/losing streak)
        // - Tendance de performance

        Map<String, Object> momentum = new HashMap<>();
        // momentum.put("last5GamesWinRate", ...);
        // momentum.put("currentStreak", ...);
        // momentum.put("trend", "improving" / "declining" / "stable");

        return momentum;
    }

    /**
     * Génère des insights basés sur les données
     *
     * INSIGHTS AUTOMATIQUES:
     * - Patterns intéressants dans les données
     * - Anomalies et outliers
     * - Recommandations pour les prédictions
     */
    public Map<String, Object> generateInsights(Long seasonId) {
        log.info("Generating insights for season: {}", seasonId);

        Dataset<Row> data = dataPreparationService.prepareGameDataset(seasonId);

        Map<String, Object> insights = new HashMap<>();

        // INSIGHT 1: Facteur le plus important pour gagner
        Map<String, Double> correlations = analyzeCorrelations(seasonId);
        String topFactor = correlations.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("unknown");

        insights.put("mostImportantFactor", topFactor);
        insights.put("factorCorrelation", correlations.get(topFactor));

        // INSIGHT 2: Équipe avec le meilleur momentum
        // (simplifié)
        insights.put("hottestTeam", "Team with best recent form");

        // INSIGHT 3: Prédictibilité de la saison
        double avgScoreDiff = data.agg(avg("score_difference")).first().getDouble(0);
        insights.put("averageScoreDifference", avgScoreDiff);
        insights.put("competitiveness", avgScoreDiff < 10 ? "very competitive" : "dominant teams");

        return insights;
    }

    /**
     * Convertit un Dataset Spark en List de Maps
     *
     * UTILITÉ:
     * Transformer les résultats Spark en format JSON-friendly
     * pour les APIs REST
     */
    private List<Map<String, Object>> convertToMap(Dataset<Row> dataset) {
        return dataset.collectAsList().stream()
                .map(row -> {
                    Map<String, Object> map = new HashMap<>();
                    for (String field : row.schema().fieldNames()) {
                        map.put(field, row.getAs(field));
                    }
                    return map;
                })
                .collect(Collectors.toList());
    }


    /**
     * Sauvegarde les résultats d'analyse en Parquet
     *
     * PARQUET:
     * Format de fichier colonne optimisé pour analytics
     * - Compression efficace
     * - Lecture rapide
     * - Compatible avec tous les outils Big Data
     *
     * UTILITÉ:
     * - Sauvegarder les résultats d'analyses longues
     * - Partager les données avec d'autres systèmes
     * - Cache de résultats précalculés
     */
    public void saveAnalysisResults(Dataset<Row> data, String path) {
        log.info("Saving analysis results to: {}", path);

        data.write()
                .mode("overwrite")  // Écraser si existe déjà
                .parquet(path);     // Format Parquet
    }

    /**
     * Charge des résultats d'analyse sauvegardés
     */
    public Dataset<Row> loadAnalysisResults(String path) {
        log.info("Loading analysis results from: {}", path);

        return sparkSession.read().parquet(path);
    }
}
