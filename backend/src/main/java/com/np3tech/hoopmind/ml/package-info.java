/**
 * Prédictions et analyses. Le reste de l'application dépend du port {@link com.np3tech.hoopmind.ml.GameOutcomePredictor} ;
 * l'implémentation Apache Spark vit dans {@code ml.spark} et peut être désactivée
 * ({@code hoopmind.ml.spark.enabled=false}) ou remplacée par un autre moteur.
 * <p>
 * Module fermé : il ne lit les données NBA qu'à travers l'API publique du module NBA ({@code GameHistory}).
 */
@org.springframework.modulith.ApplicationModule(displayName = "Prédictions (ML)")
package com.np3tech.hoopmind.ml;
