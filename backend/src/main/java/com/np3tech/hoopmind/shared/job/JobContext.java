package com.np3tech.hoopmind.shared.job;

import java.util.UUID;

/** Ce qu'un job peut faire pendant son exécution : signaler son avancement. */
public interface JobContext {

    UUID jobId();

    /** Change d'étape (enregistré en base). */
    void step(String step);

    /** Avancement dans l'étape en cours (gardé en mémoire : il change trop souvent pour la base). */
    void progress(Long done, Long total);
}
