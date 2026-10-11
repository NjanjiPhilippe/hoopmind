package com.np3tech.hoopmind.ml;

import java.time.LocalDate;

/**
 * Port de prédiction du vainqueur d'un match. L'implémentation actuelle applique en Java le modèle entraîné par
 * Spark ML ; une autre (modèle privé, service externe) peut la remplacer sans toucher au reste de l'application.
 */
public interface GameOutcomePredictor {

    /**
     * @param date    jour du match : la forme des équipes est calculée avec les seuls matchs joués avant
     * @param playoff match de playoffs
     */
    GameOutcomePrediction predict(long seasonId, long homeTeamId, long awayTeamId, LocalDate date, boolean playoff);
}
