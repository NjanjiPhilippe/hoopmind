package com.np3tech.hoopmind.ml;

import com.np3tech.hoopmind.auth.CurrentUser;

/**
 * Fin d'un entraînement du modèle de prédiction, publiée avec {@code ApplicationEventPublisher}.
 * L'administrateur qui l'a lancé est transporté dans l'événement (l'entraînement peut finir hors requête).
 */
public sealed interface ModelTrainingEvent {

    CurrentUser requestedBy();

    default boolean failed() {
        return false;
    }

    record ModelTrained(CurrentUser requestedBy, String version, double accuracy, String testSeason)
            implements ModelTrainingEvent {
    }

    /** {@code error} : message déjà présentable à un administrateur. */
    record ModelTrainingFailed(CurrentUser requestedBy, String error) implements ModelTrainingEvent {
        @Override
        public boolean failed() {
            return true;
        }
    }
}
