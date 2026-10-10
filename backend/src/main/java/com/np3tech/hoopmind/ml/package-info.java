/**
 * Prédictions et analyses. Le reste de l'application dépend du port {@link com.np3tech.hoopmind.ml.GameOutcomePredictor} ;
 * l'implémentation Apache Spark vit dans {@code ml.spark} et peut être désactivée
 * ({@code hoopmind.ml.spark.enabled=false}) ou remplacée par un autre moteur.
 * <p>
 * Module ouvert pour l'instant : le code existant utilise directement les entités et dépôts du module NBA.
 * Il sera refermé lors du chantier ML.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Prédictions (ML)", type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package com.np3tech.hoopmind.ml;
