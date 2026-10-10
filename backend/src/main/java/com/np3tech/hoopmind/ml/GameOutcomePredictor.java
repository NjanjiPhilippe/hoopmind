package com.np3tech.hoopmind.ml;

/**
 * Port de prédiction du vainqueur d'un match. L'implémentation actuelle s'appuie sur Spark ML ;
 * une autre (modèle privé, service externe) peut la remplacer sans toucher au reste de l'application.
 */
public interface GameOutcomePredictor {

    GameOutcomePrediction predict(Long seasonId, Long homeTeamId, Long awayTeamId);
}
