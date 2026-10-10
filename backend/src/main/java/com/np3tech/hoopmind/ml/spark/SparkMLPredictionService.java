package com.np3tech.hoopmind.ml.spark;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.classification.LogisticRegression;
import org.apache.spark.ml.classification.RandomForestClassifier;
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator;
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator;
import org.apache.spark.ml.feature.*;
import org.apache.spark.ml.linalg.Vector;
import org.apache.spark.ml.regression.LinearRegression;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Service de Machine Learning avec Spark MLlib
 *
 * SPARK MLLIB:
 * Bibliothèque de Machine Learning distribuée de Spark
 * - Scalable: traite des millions de données
 * - Optimisée: exécution parallèle sur cluster
 * - Complète: classification, régression, clustering, etc.
 *
 * CONCEPTS CLÉS:
 *
 * 1. PIPELINE:
 *    Chaîne de transformations et d'algorithmes ML
 *    Exemple: Normalisation → Vectorisation → Modèle
 *    Avantages: reproductibilité, modularité, optimisation
 *
 * 2. FEATURES:
 *    Variables d'entrée du modèle (stats des équipes)
 *
 * 3. LABEL:
 *    Variable à prédire (gagnant du match)
 *
 * 4. TRAIN/TEST SPLIT:
 *    Division des données pour entraînement et validation
 */
@Service
@ConditionalOnSpark
@RequiredArgsConstructor
@Slf4j
public class SparkMLPredictionService {

    private final SparkSession sparkSession;
    private final SparkDataPreparationService dataPreparationService;

    // Chemins de sauvegarde des modèles
    private static final String MODEL_PATH = "spark-models/";

    /**
     * Entraîne un modèle de classification (Random Forest)
     *
     * RANDOM FOREST:
     * - Ensemble de nombreux arbres de décision
     * - Chaque arbre vote pour la prédiction finale
     * - Robuste aux overfitting (sur-apprentissage)
     * - Fonctionne bien avec des features corrélées
     *
     * PROCESSUS D'ENTRAÎNEMENT:
     * 1. Préparer les données
     * 2. Créer le pipeline de transformation
     * 3. Entraîner le modèle
     * 4. Évaluer les performances
     * 5. Sauvegarder le modèle
     *
     * @param seasonId Saison pour l'entraînement
     * @return Métriques de performance du modèle
     */
    public Map<String, Object> trainRandomForestClassifier(Long seasonId) {
        log.info("Training Random Forest classifier for season: {}", seasonId);

        // 1. PRÉPARATION DES DONNÉES
        Dataset<Row> data = dataPreparationService.prepareGameDataset(seasonId);
        data = dataPreparationService.cleanAndNormalizeData(data);

        // 2. SÉLECTION DES FEATURES
        // Colonnes qui serviront de variables d'entrée au modèle
        String[] featureColumns = new String[]{
                "home_wins", "home_losses", "home_win_pct",
                "home_ppg", "home_oppg", "home_off_rating", "home_def_rating",
                "away_wins", "away_losses", "away_win_pct",
                "away_ppg", "away_oppg", "away_off_rating", "away_def_rating",
                "win_pct_diff", "off_rating_diff", "def_rating_diff",
                "net_rating_diff", "ppg_diff", "is_playoff"
        };

        // 3. CRÉER LE PIPELINE
        // Le pipeline enchaîne les transformations de manière ordonnée
        Pipeline pipeline = createClassificationPipeline(featureColumns);

        // 4. TRAIN/TEST SPLIT
        // 80% pour l'entraînement, 20% pour la validation
        // Seed pour la reproductibilité
        Dataset<Row>[] splits = data.randomSplit(new double[]{0.8, 0.2}, 12345L);
        Dataset<Row> trainingData = splits[0];
        Dataset<Row> testData = splits[1];

        log.info("Training set size: {}, Test set size: {}",
                trainingData.count(), testData.count());

        // 5. ENTRAÎNER LE MODÈLE
        // fit() entraîne le modèle sur les données d'entraînement
        PipelineModel model = pipeline.fit(trainingData);

        // 6. FAIRE DES PRÉDICTIONS
        // transform() applique le modèle aux données de test
        Dataset<Row> predictions = model.transform(testData);

        // 7. ÉVALUER LES PERFORMANCES
        Map<String, Object> metrics = evaluateClassificationModel(predictions);

        // 8. SAUVEGARDER LE MODÈLE
        try {
            String modelPath = MODEL_PATH + "random_forest_season_" + seasonId;
            model.write().overwrite().save(modelPath);
            log.info("Model saved to: {}", modelPath);
            metrics.put("modelPath", modelPath);
        } catch (IOException e) {
            log.error("Error saving model", e);
        }

        return metrics;
    }

    /**
     * Crée un pipeline de classification
     *
     * ÉTAPES DU PIPELINE:
     *
     * 1. VectorAssembler:
     *    Combine toutes les features en un seul vecteur
     *    Input: colonnes séparées
     *    Output: colonne "features" avec vecteur
     *
     * 2. StandardScaler:
     *    Normalise les features (moyenne=0, écart-type=1)
     *    Important car les algorithmes ML sont sensibles à l'échelle
     *
     * 3. RandomForestClassifier:
     *    L'algorithme de prédiction
     *    - numTrees: nombre d'arbres dans la forêt
     *    - maxDepth: profondeur maximale de chaque arbre
     *    - plus d'arbres = meilleure précision mais plus lent
     */
    private Pipeline createClassificationPipeline(String[] featureColumns) {
        // Étape 1: Assembler les features
        VectorAssembler assembler = new VectorAssembler()
                .setInputCols(featureColumns)
                .setOutputCol("rawFeatures")
                .setHandleInvalid("skip"); // Ignore les lignes avec valeurs invalides

        // Étape 2: Normaliser les features
        StandardScaler scaler = new StandardScaler()
                .setInputCol("rawFeatures")
                .setOutputCol("features")
                .setWithMean(true)  // Centrer autour de 0
                .setWithStd(true);  // Mettre écart-type à 1

        // Étape 3: Algorithme Random Forest
        RandomForestClassifier rf = new RandomForestClassifier()
                .setLabelCol("home_team_won")  // Ce qu'on veut prédire
                .setFeaturesCol("features")     // Les variables d'entrée
                .setPredictionCol("prediction") // Colonne de sortie
                .setNumTrees(100)               // 100 arbres
                .setMaxDepth(10)                // Profondeur max = 10
                .setSeed(12345L);               // Pour reproductibilité

        // Créer le pipeline avec les 3 étapes
        return new Pipeline().setStages(new PipelineStage[]{
                assembler, scaler, rf
        });
    }

    /**
     * Entraîne un modèle de régression logistique
     *
     * RÉGRESSION LOGISTIQUE:
     * - Algorithme simple et rapide
     * - Prédit des probabilités (0 à 1)
     * - Interprétable: on peut voir l'importance de chaque feature
     * - Bon pour comprendre les relations entre variables
     *
     * QUAND L'UTILISER:
     * - Problèmes de classification binaire (victoire/défaite)
     * - Quand on veut des probabilités précises
     * - Baseline simple avant des modèles plus complexes
     */
    public Map<String, Object> trainLogisticRegressionModel(Long seasonId) {
        log.info("Training Logistic Regression model for season: {}", seasonId);

        Dataset<Row> data = dataPreparationService.prepareGameDataset(seasonId);
        data = dataPreparationService.cleanAndNormalizeData(data);

        String[] featureColumns = new String[]{
                "win_pct_diff", "net_rating_diff", "ppg_diff",
                "off_rating_diff", "def_rating_diff", "pace_diff"
        };

        // Assembler et normaliser
        VectorAssembler assembler = new VectorAssembler()
                .setInputCols(featureColumns)
                .setOutputCol("rawFeatures");

        StandardScaler scaler = new StandardScaler()
                .setInputCol("rawFeatures")
                .setOutputCol("features")
                .setWithMean(true)
                .setWithStd(true);

        // Régression logistique
        LogisticRegression lr = new LogisticRegression()
                .setLabelCol("home_team_won")
                .setFeaturesCol("features")
                .setMaxIter(100)        // Nombre max d'itérations
                .setRegParam(0.01)      // Régularisation (évite overfitting)
                .setElasticNetParam(0.5); // Mix L1/L2 régularisation

        Pipeline pipeline = new Pipeline().setStages(new PipelineStage[]{
                assembler, scaler, lr
        });

        // Train/test split
        Dataset<Row>[] splits = data.randomSplit(new double[]{0.8, 0.2}, 12345L);

        // Entraîner
        PipelineModel model = pipeline.fit(splits[0]);
        Dataset<Row> predictions = model.transform(splits[1]);

        // Évaluer
        Map<String, Object> metrics = evaluateClassificationModel(predictions);

        // Sauvegarder
        try {
            String modelPath = MODEL_PATH + "logistic_regression_season_" + seasonId;
            model.write().overwrite().save(modelPath);
            metrics.put("modelPath", modelPath);
        } catch (IOException e) {
            log.error("Error saving model", e);
        }

        return metrics;
    }

    /**
     * Entraîne un modèle de régression pour prédire les scores
     *
     * RÉGRESSION LINÉAIRE:
     * - Prédit des valeurs continues (scores)
     * - Trouve la relation linéaire entre features et label
     * - Utile pour prédire les points marqués
     *
     * UTILITÉ:
     * - Prédire le score total d'un match
     * - Estimer la différence de points
     * - Calculer les probabilités over/under pour les paris
     */
    public Map<String, Object> trainScorePredictionModel(Long seasonId) {
        log.info("Training score prediction model for season: {}", seasonId);

        Dataset<Row> data = dataPreparationService.prepareGameDataset(seasonId);
        data = dataPreparationService.cleanAndNormalizeData(data);

        String[] featureColumns = new String[]{
                "home_ppg", "home_oppg", "away_ppg", "away_oppg",
                "home_pace", "away_pace", "avg_ppg", "combined_strength"
        };

        VectorAssembler assembler = new VectorAssembler()
                .setInputCols(featureColumns)
                .setOutputCol("features");

        // Régression linéaire pour prédire le score total
        LinearRegression lr = new LinearRegression()
                .setLabelCol("total_score")  // On prédit le score total
                .setFeaturesCol("features")
                .setMaxIter(100)
                .setRegParam(0.01);

        Pipeline pipeline = new Pipeline().setStages(new PipelineStage[]{
                assembler, lr
        });

        Dataset<Row>[] splits = data.randomSplit(new double[]{0.8, 0.2}, 12345L);
        PipelineModel model = pipeline.fit(splits[0]);
        Dataset<Row> predictions = model.transform(splits[1]);

        // Évaluer avec métriques de régression
        Map<String, Object> metrics = evaluateRegressionModel(predictions);

        try {
            String modelPath = MODEL_PATH + "score_prediction_season_" + seasonId;
            model.write().overwrite().save(modelPath);
            metrics.put("modelPath", modelPath);
        } catch (IOException e) {
            log.error("Error saving model", e);
        }

        return metrics;
    }

    /**
     * Évalue un modèle de classification
     *
     * MÉTRIQUES:
     *
     * 1. ACCURACY (Précision globale):
     *    % de prédictions correctes
     *    Exemple: 75% = 75 matchs bien prédits sur 100
     *
     * 2. PRECISION:
     *    Parmi les "victoires domicile" prédites, combien sont vraies?
     *    Important si on veut éviter les faux positifs
     *
     * 3. RECALL (Rappel):
     *    Parmi les vraies "victoires domicile", combien sont détectées?
     *    Important si on veut détecter toutes les victoires
     *
     * 4. F1-SCORE:
     *    Moyenne harmonique de precision et recall
     *    Bon indicateur global de performance
     *
     * 5. AUC-ROC:
     *    Area Under Curve - Receiver Operating Characteristic
     *    Mesure la capacité à distinguer les classes
     *    1.0 = parfait, 0.5 = hasard
     */
    private Map<String, Object> evaluateClassificationModel(Dataset<Row> predictions) {
        Map<String, Object> metrics = new HashMap<>();

        // Accuracy
        MulticlassClassificationEvaluator accuracyEvaluator =
                new MulticlassClassificationEvaluator()
                        .setLabelCol("home_team_won")
                        .setPredictionCol("prediction")
                        .setMetricName("accuracy");

        double accuracy = accuracyEvaluator.evaluate(predictions);
        metrics.put("accuracy", accuracy);
        log.info("Model Accuracy: {}", accuracy);

        // Precision
        MulticlassClassificationEvaluator precisionEvaluator =
                new MulticlassClassificationEvaluator()
                        .setLabelCol("home_team_won")
                        .setPredictionCol("prediction")
                        .setMetricName("weightedPrecision");

        double precision = precisionEvaluator.evaluate(predictions);
        metrics.put("precision", precision);
        log.info("Model Precision: {}", precision);

        // Recall
        MulticlassClassificationEvaluator recallEvaluator =
                new MulticlassClassificationEvaluator()
                        .setLabelCol("home_team_won")
                        .setPredictionCol("prediction")
                        .setMetricName("weightedRecall");

        double recall = recallEvaluator.evaluate(predictions);
        metrics.put("recall", recall);
        log.info("Model Recall: {}", recall);

        // F1-Score
        MulticlassClassificationEvaluator f1Evaluator =
                new MulticlassClassificationEvaluator()
                        .setLabelCol("home_team_won")
                        .setPredictionCol("prediction")
                        .setMetricName("f1");

        double f1 = f1Evaluator.evaluate(predictions);
        metrics.put("f1Score", f1);
        log.info("Model F1-Score: {}", f1);

        // AUC-ROC
        BinaryClassificationEvaluator aucEvaluator =
                new BinaryClassificationEvaluator()
                        .setLabelCol("home_team_won")
                        .setRawPredictionCol("rawPrediction")
                        .setMetricName("areaUnderROC");

        double auc = aucEvaluator.evaluate(predictions);
        metrics.put("auc", auc);
        log.info("Model AUC: {}", auc);

        return metrics;
    }

    /**
     * Évalue un modèle de régression
     *
     * MÉTRIQUES DE RÉGRESSION:
     *
     * 1. RMSE (Root Mean Squared Error):
     *    Erreur moyenne en unités du label
     *    Exemple: RMSE=10 points → erreur moyenne de ±10 points
     *
     * 2. MAE (Mean Absolute Error):
     *    Moyenne des erreurs absolues
     *    Moins sensible aux outliers que RMSE
     *
     * 3. R² (Coefficient de détermination):
     *    % de variance expliquée par le modèle
     *    1.0 = prédictions parfaites
     *    0.0 = modèle pas mieux que la moyenne
     */
    private Map<String, Object> evaluateRegressionModel(Dataset<Row> predictions) {
        Map<String, Object> metrics = new HashMap<>();

        org.apache.spark.ml.evaluation.RegressionEvaluator rmseEvaluator =
                new org.apache.spark.ml.evaluation.RegressionEvaluator()
                        .setLabelCol("total_score")
                        .setPredictionCol("prediction")
                        .setMetricName("rmse");

        double rmse = rmseEvaluator.evaluate(predictions);
        metrics.put("rmse", rmse);
        log.info("Model RMSE: {}", rmse);

        org.apache.spark.ml.evaluation.RegressionEvaluator maeEvaluator =
                new org.apache.spark.ml.evaluation.RegressionEvaluator()
                        .setLabelCol("total_score")
                        .setPredictionCol("prediction")
                        .setMetricName("mae");

        double mae = maeEvaluator.evaluate(predictions);
        metrics.put("mae", mae);
        log.info("Model MAE: {}", mae);

        org.apache.spark.ml.evaluation.RegressionEvaluator r2Evaluator =
                new org.apache.spark.ml.evaluation.RegressionEvaluator()
                        .setLabelCol("total_score")
                        .setPredictionCol("prediction")
                        .setMetricName("r2");

        double r2 = r2Evaluator.evaluate(predictions);
        metrics.put("r2", r2);
        log.info("Model R²: {}", r2);

        return metrics;
    }

    /**
     * Charge un modèle sauvegardé
     *
     * UTILITÉ:
     * - Utiliser un modèle pré-entraîné pour faire des prédictions
     * - Pas besoin de ré-entraîner à chaque fois
     * - Déploiement en production
     */
    public PipelineModel loadModel(String modelPath) throws IOException {
        log.info("Loading model from: {}", modelPath);
        return PipelineModel.load(modelPath);
    }

    /**
     * Fait une prédiction pour un match spécifique
     *
     * PROCESSUS:
     * 1. Charger le modèle
     * 2. Préparer les features du match
     * 3. Appliquer le modèle
     * 4. Retourner la prédiction et la probabilité
     */
    public Map<String, Object> predictGameOutcome(Long seasonId,
                                                  Long homeTeamId,
                                                  Long awayTeamId) {
        try {
            String modelPath = MODEL_PATH + "random_forest_season_" + seasonId;
            PipelineModel model = loadModel(modelPath);

            // Créer un dataset avec les features du match
            // (code simplifié - en réalité il faut récupérer toutes les stats)
            Dataset<Row> matchData = createMatchFeatures(homeTeamId, awayTeamId, seasonId);

            // Faire la prédiction
            Dataset<Row> prediction = model.transform(matchData);

            // Extraire les résultats
            Row result = prediction.first();
            double predictedClass = result.getAs("prediction");
            Vector probability = result.getAs("probability");

            Map<String, Object> predictionResult = new HashMap<>();
            predictionResult.put("homeTeamWins", predictedClass == 1.0);
            predictionResult.put("homeWinProbability", probability.apply(1));
            predictionResult.put("awayWinProbability", probability.apply(0));

            return predictionResult;

        } catch (Exception e) {
            log.error("Error predicting game outcome", e);
            throw new RuntimeException("Prediction failed", e);
        }
    }

    /**
     * Crée les features pour un match à prédire
     * (Méthode simplifiée - à implémenter complètement)
     */
    private Dataset<Row> createMatchFeatures(Long homeTeamId, Long awayTeamId, Long seasonId) {
        // TODO: Implémenter la récupération et transformation des features
        return null;
    }
}
