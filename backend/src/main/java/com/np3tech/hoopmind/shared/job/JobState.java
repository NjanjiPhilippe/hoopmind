package com.np3tech.hoopmind.shared.job;

import java.time.Instant;

/**
 * État d'un job. Type scellé : un {@code switch} sur l'état est exhaustif, sans branche {@code default},
 * et ajouter un état oblige à traiter chaque usage.
 *
 * @param <R> type du résultat d'un job réussi
 */
public sealed interface JobState<R> {

    /** @param step étape en cours (propre au type de job) ; {@code done}/{@code total} : avancement, si connu */
    record Running<R>(String step, Long done, Long total) implements JobState<R> {
    }

    record Succeeded<R>(Instant finishedAt, R result) implements JobState<R> {
    }

    /** @param error message présentable à un administrateur (ni chemin ni trace technique) */
    record Failed<R>(Instant finishedAt, String step, String error) implements JobState<R> {
    }

    /** Le serveur s'est arrêté pendant le job : il n'a ni réussi ni échoué, il faut le relancer. */
    record Interrupted<R>(Instant detectedAt, String step) implements JobState<R> {
    }

    default boolean running() {
        return this instanceof Running;
    }
}
