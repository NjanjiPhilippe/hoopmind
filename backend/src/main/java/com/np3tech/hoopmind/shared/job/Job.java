package com.np3tech.hoopmind.shared.job;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Un job en arrière-plan et son état.
 *
 * @param requestedBy administrateur qui l'a lancé, {@code null} pour un job lancé par le système (démarrage)
 * @param parameters  paramètres de lancement, affichables
 */
public record Job<R>(UUID id, JobType type, Requester requestedBy, Map<String, String> parameters, Instant createdAt,
                     JobState<R> state) {

    public Job {
        parameters = Map.copyOf(parameters);
    }

    /** Auteur d'un job, capturé au lancement : le job tourne ensuite hors de la requête HTTP. */
    public record Requester(Long id, String login) {
    }
}
