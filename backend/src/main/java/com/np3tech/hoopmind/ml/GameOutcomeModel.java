package com.np3tech.hoopmind.ml;

import java.util.Optional;

/**
 * Port d'entraînement et de suivi du modèle de prédiction des matchs.
 * <p>
 * L'API web et les autres modules ne connaissent que ce contrat : l'implémentation Spark ML ({@code ml.spark})
 * peut être désactivée ou remplacée (modèle privé, service externe) sans toucher aux appelants.
 */
public interface GameOutcomeModel {

    /**
     * Entraîne le modèle sur les saisons les plus récentes et l'évalue sur la dernière.
     *
     * @param seasons nombre de saisons à utiliser ; {@code null} reprend le réglage par défaut
     */
    TrainingReport train(Integer seasons);

    /** Dernier entraînement enregistré, s'il y en a un. */
    Optional<TrainingReport> latestReport();
}
