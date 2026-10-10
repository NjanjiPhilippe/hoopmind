package com.np3tech.hoopmind.ml.spark;

import com.np3tech.hoopmind.nba.entity.*;
import com.np3tech.hoopmind.nba.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static org.apache.spark.sql.functions.*;

/**
 * Service de préparation des données pour Spark
 *
 * RÔLE PRINCIPAL:
 * Transformer les données de la base SQL en DataFrames Spark optimisés
 * pour l'analyse et le Machine Learning
 *
 * CONCEPTS CLÉS:
 *
 * 1. DATASET<ROW> (DataFrame):
 *    - Structure de données distribuée et optimisée
 *    - Similaire à une table SQL mais distribué sur plusieurs machines
 *    - Permet des opérations SQL et fonctionnelles
 *    - Optimisé par Catalyst (moteur d'optimisation de Spark)
 *
 * 2. SCHEMA:
 *    - Définit la structure des données (colonnes et types)
 *    - Permet l'optimisation et la validation des données
 *    - Important pour les performances
 *
 * 3. TRANSFORMATIONS:
 *    - Opérations lazy (exécutées seulement quand nécessaire)
 *    - Chaînables pour créer des pipelines de traitement
 *    - Optimisées automatiquement par Spark
 */
@Service
@ConditionalOnSpark
@RequiredArgsConstructor
@Slf4j
public class SparkDataPreparationService {

    private final SparkSession sparkSession;
    private final GameRepository gameRepository;
    private final GameResultRepository gameResultRepository;
    private final TeamStatsRepository teamStatsRepository;
    private final PlayerStatsRepository playerStatsRepository;

    /**
     * Prépare les données des matchs avec leurs statistiques
     *
     * EXPLICATION DÉTAILLÉE:
     *
     * Cette méthode collecte toutes les informations pertinentes pour prédire
     * le résultat d'un match:
     * - Statistiques historiques des équipes
     * - Résultats des matchs précédents
     * - Forme récente des équipes
     * - Statistiques des joueurs clés
     *
     * PROCESSUS:
     * 1. Récupérer les données de la DB
     * 2. Convertir en format Row (format Spark)
     * 3. Créer un DataFrame avec schéma défini
     * 4. Ajouter des features calculées
     */
    public Dataset<Row> prepareGameDataset(Long seasonId) {
        log.info("Preparing game dataset for season: {}", seasonId);

        // 1. RÉCUPÉRATION DES DONNÉES
        List<Game> games = gameRepository.findBySeason(
                new Season().toBuilder().id(seasonId).build()
        );

        // 2. CONVERSION EN ROWS SPARK
        // Row = unité de base dans un DataFrame Spark
        List<Row> rows = new ArrayList<>();

        for (Game game : games) {
            GameResult result = gameResultRepository.findByGame(game).orElse(null);

            if (result != null) {
                // Récupérer les stats des équipes
                TeamStats homeStats = getTeamStats(game.getHomeTeam().getId(), seasonId);
                TeamStats awayStats = getTeamStats(game.getAwayTeam().getId(), seasonId);

                // Créer une Row avec toutes les features
                Row row = createGameRow(game, result, homeStats, awayStats);
                rows.add(row);
            }
        }

        // 3. DÉFINIR LE SCHÉMA
        // Le schéma définit la structure exacte des données
        StructType schema = createGameSchema();

        // 4. CRÉER LE DATAFRAME
        Dataset<Row> dataset = sparkSession.createDataFrame(rows, schema);

        // 5. AJOUTER DES FEATURES CALCULÉES
        // Feature Engineering: créer de nouvelles colonnes à partir des existantes
        dataset = addCalculatedFeatures(dataset);

        log.info("Game dataset prepared with {} rows", dataset.count());
        return dataset;
    }

    /**
     * Crée le schéma pour les données de match
     *
     * TYPES DE DONNÉES SPARK:
     * - LongType: Pour les IDs et grands nombres entiers
     * - IntegerType: Pour les statistiques numériques
     * - DoubleType: Pour les pourcentages et décimales
     * - StringType: Pour les noms et identifiants texte
     * - BooleanType: Pour les indicateurs vrai/faux
     *
     * IMPORTANCE DU SCHÉMA:
     * - Validation des données
     * - Optimisation des requêtes
     * - Sérialisation efficace
     * - Détection précoce des erreurs
     */
    private StructType createGameSchema() {
        return new StructType(new StructField[]{
                // IDs
                DataTypes.createStructField("game_id", DataTypes.LongType, false),
                DataTypes.createStructField("home_team_id", DataTypes.LongType, false),
                DataTypes.createStructField("away_team_id", DataTypes.LongType, false),

                // Résultat du match (label pour ML)
                DataTypes.createStructField("home_team_won", DataTypes.IntegerType, false), // 1 si victoire domicile, 0 sinon
                DataTypes.createStructField("score_difference", DataTypes.IntegerType, false),
                DataTypes.createStructField("total_score", DataTypes.IntegerType, false),

                // Stats équipe domicile
                DataTypes.createStructField("home_wins", DataTypes.IntegerType, true),
                DataTypes.createStructField("home_losses", DataTypes.IntegerType, true),
                DataTypes.createStructField("home_win_pct", DataTypes.DoubleType, true),
                DataTypes.createStructField("home_ppg", DataTypes.DoubleType, true),
                DataTypes.createStructField("home_oppg", DataTypes.DoubleType, true),
                DataTypes.createStructField("home_off_rating", DataTypes.DoubleType, true),
                DataTypes.createStructField("home_def_rating", DataTypes.DoubleType, true),
                DataTypes.createStructField("home_net_rating", DataTypes.DoubleType, true),
                DataTypes.createStructField("home_pace", DataTypes.DoubleType, true),

                // Stats équipe extérieure
                DataTypes.createStructField("away_wins", DataTypes.IntegerType, true),
                DataTypes.createStructField("away_losses", DataTypes.IntegerType, true),
                DataTypes.createStructField("away_win_pct", DataTypes.DoubleType, true),
                DataTypes.createStructField("away_ppg", DataTypes.DoubleType, true),
                DataTypes.createStructField("away_oppg", DataTypes.DoubleType, true),
                DataTypes.createStructField("away_off_rating", DataTypes.DoubleType, true),
                DataTypes.createStructField("away_def_rating", DataTypes.DoubleType, true),
                DataTypes.createStructField("away_net_rating", DataTypes.DoubleType, true),
                DataTypes.createStructField("away_pace", DataTypes.DoubleType, true),

                // Features contextuelles
                DataTypes.createStructField("is_playoff", DataTypes.IntegerType, false),
                DataTypes.createStructField("is_national_tv", DataTypes.IntegerType, false)
        });
    }

    /**
     * Crée une Row Spark à partir des données de match
     *
     * EXPLICATION:
     * Une Row est l'équivalent Spark d'une ligne de base de données.
     * Chaque élément doit correspondre exactement au schéma défini.
     *
     * ORDRE IMPORTANT:
     * Les valeurs doivent être dans le même ordre que le schéma!
     */
    private Row createGameRow(Game game, GameResult result,
                              TeamStats homeStats, TeamStats awayStats) {
        return RowFactory.create(
                // IDs
                game.getId(),
                game.getHomeTeam().getId(),
                game.getAwayTeam().getId(),

                // Label (ce qu'on veut prédire)
                result.getWinner().getId().equals(game.getHomeTeam().getId()) ? 1 : 0,
                result.getScoreDifference(),
                result.getHomeTeamScore() + result.getAwayTeamScore(),

                // Stats équipe domicile
                homeStats != null ? homeStats.getWins() : 0,
                homeStats != null ? homeStats.getLosses() : 0,
                homeStats != null ? homeStats.getWinPercentage().doubleValue() : 0.0,
                homeStats != null ? homeStats.getPointsPerGame().doubleValue() : 0.0,
                homeStats != null ? homeStats.getPointsAllowedPerGame().doubleValue() : 0.0,
                homeStats != null ? homeStats.getOffensiveRating().doubleValue() : 0.0,
                homeStats != null ? homeStats.getDefensiveRating().doubleValue() : 0.0,
                homeStats != null ? homeStats.getNetRating().doubleValue() : 0.0,
                homeStats != null ? homeStats.getPace().doubleValue() : 0.0,

                // Stats équipe extérieure
                awayStats != null ? awayStats.getWins() : 0,
                awayStats != null ? awayStats.getLosses() : 0,
                awayStats != null ? awayStats.getWinPercentage().doubleValue() : 0.0,
                awayStats != null ? awayStats.getPointsPerGame().doubleValue() : 0.0,
                awayStats != null ? awayStats.getPointsAllowedPerGame().doubleValue() : 0.0,
                awayStats != null ? awayStats.getOffensiveRating().doubleValue() : 0.0,
                awayStats != null ? awayStats.getDefensiveRating().doubleValue() : 0.0,
                awayStats != null ? awayStats.getNetRating().doubleValue() : 0.0,
                awayStats != null ? awayStats.getPace().doubleValue() : 0.0,

                // Features contextuelles
                game.getGameType().name().contains("PLAYOFF") ? 1 : 0,
                game.getIsNationalTv() != null && game.getIsNationalTv() ? 1 : 0
        );
    }

    /**
     * Ajoute des features calculées au DataFrame
     *
     * FEATURE ENGINEERING:
     * Processus de création de nouvelles variables (features) à partir des existantes
     * pour améliorer les prédictions du modèle ML
     *
     * EXEMPLES DE FEATURES:
     * - Différences entre équipes (win_pct_diff, rating_diff)
     * - Ratios et combinaisons de stats
     * - Indicateurs de tendance
     *
     * FONCTIONS SPARK UTILISÉES:
     * - col(): Référence une colonne
     * - minus(): Soustraction
     * - withColumn(): Ajoute une nouvelle colonne
     * - alias(): Renomme une colonne
     */
    private Dataset<Row> addCalculatedFeatures(Dataset<Row> dataset) {
        return dataset
                // Différence de pourcentage de victoires
                .withColumn("win_pct_diff",
                        col("home_win_pct").minus(col("away_win_pct")))

                // Différence de rating offensif
                .withColumn("off_rating_diff",
                        col("home_off_rating").minus(col("away_off_rating")))

                // Différence de rating défensif (négatif = meilleur)
                .withColumn("def_rating_diff",
                        col("home_def_rating").minus(col("away_def_rating")))

                // Différence de net rating (indicateur global de force)
                .withColumn("net_rating_diff",
                        col("home_net_rating").minus(col("away_net_rating")))

                // Différence de points par match
                .withColumn("ppg_diff",
                        col("home_ppg").minus(col("away_ppg")))

                // Moyenne des points par match (indique le tempo du match)
                .withColumn("avg_ppg",
                        col("home_ppg").plus(col("away_ppg")).divide(lit(2.0)))

                // Différence de pace (vitesse de jeu)
                .withColumn("pace_diff",
                        col("home_pace").minus(col("away_pace")))

                // Force combinée des deux équipes (pour prédire le score total)
                .withColumn("combined_strength",
                        col("home_win_pct").plus(col("away_win_pct")).divide(lit(2.0)));
    }

    /**
     * Prépare un dataset pour les statistiques des joueurs
     *
     * UTILITÉ:
     * Analyser les performances individuelles et leur impact sur les résultats
     */
    public Dataset<Row> preparePlayerStatsDataset(Long seasonId) {
        log.info("Preparing player stats dataset for season: {}", seasonId);

        List<PlayerStats> playerStats = playerStatsRepository.findBySeason(
                new Season().toBuilder().id(seasonId).build()
        );

        List<Row> rows = new ArrayList<>();

        for (PlayerStats stats : playerStats) {
            Row row = RowFactory.create(
                    stats.getPlayer().getId(),
                    stats.getPlayer().getTeam().getId(),
                    stats.getGamesPlayed(),
                    stats.getPointsPerGame().doubleValue(),
                    stats.getAssistsPerGame().doubleValue(),
                    stats.getTotalReboundsPerGame().doubleValue(),
                    stats.getFieldGoalPercentage().doubleValue(),
                    stats.getThreePointPercentage().doubleValue(),
                    stats.getTrueShootingPercentage().doubleValue(),
                    stats.getPlayerEfficiencyRating().doubleValue()
            );
            rows.add(row);
        }

        StructType schema = new StructType(new StructField[]{
                DataTypes.createStructField("player_id", DataTypes.LongType, false),
                DataTypes.createStructField("team_id", DataTypes.LongType, false),
                DataTypes.createStructField("games_played", DataTypes.IntegerType, true),
                DataTypes.createStructField("ppg", DataTypes.DoubleType, true),
                DataTypes.createStructField("apg", DataTypes.DoubleType, true),
                DataTypes.createStructField("rpg", DataTypes.DoubleType, true),
                DataTypes.createStructField("fg_pct", DataTypes.DoubleType, true),
                DataTypes.createStructField("three_pct", DataTypes.DoubleType, true),
                DataTypes.createStructField("ts_pct", DataTypes.DoubleType, true),
                DataTypes.createStructField("per", DataTypes.DoubleType, true)
        });

        Dataset<Row> dataset = sparkSession.createDataFrame(rows, schema);

        log.info("Player stats dataset prepared with {} rows", dataset.count());
        return dataset;
    }

    /**
     * Obtient les statistiques d'une équipe
     *
     * Helper method pour récupérer les stats depuis la DB
     */
    private TeamStats getTeamStats(Long teamId, Long seasonId) {
        return teamStatsRepository.findByTeamAndSeason(
                new Team().toBuilder().id(teamId).build(),
                new Season().toBuilder().id(seasonId).build()
        ).orElse(null);
    }

    /**
     * Nettoie et normalise les données
     *
     * DATA CLEANING:
     * - Remplace les valeurs nulles
     * - Supprime les outliers
     * - Normalise les échelles de valeurs
     *
     * IMPORTANCE:
     * Des données propres = meilleurs modèles ML
     */
    public Dataset<Row> cleanAndNormalizeData(Dataset<Row> dataset) {
        log.info("Cleaning and normalizing dataset");

        // Remplacer les nulls par 0
        dataset = dataset.na().fill(0.0);

        // Supprimer les lignes avec des données impossibles
        dataset = dataset.filter(
                col("home_ppg").geq(0)
                        .and(col("away_ppg").geq(0))
                        .and(col("home_win_pct").between(0, 1))
                        .and(col("away_win_pct").between(0, 1))
        );

        return dataset;
    }
}
