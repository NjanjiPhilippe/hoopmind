package com.np3tech.hoopmind.ml.explanation;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Nombre d'appels au LLM permis à chaque utilisateur par heure (fenêtre fixe, en mémoire).
 * <p>
 * Le quota gratuit du fournisseur est partagé par tous : sans limite, un seul compte qui change la date du match à
 * chaque requête le viderait pour la journée. Au-delà de la limite, l'utilisateur reçoit l'explication rédigée sans
 * LLM : il n'est jamais bloqué. Comme le frein des connexions, l'état est perdu au redémarrage et propre à l'instance.
 */
final class LlmCallQuota {

    static final Duration WINDOW = Duration.ofHours(1);

    /** Au-delà, on oublie les fenêtres terminées avant d'en ouvrir une nouvelle. */
    private static final int MAX_TRACKED_USERS = 10_000;

    private record Window(Instant start, int calls) {
    }

    private final int maxCallsPerWindow;
    private final Clock clock;
    private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();

    LlmCallQuota(int maxCallsPerWindow, Clock clock) {
        this.maxCallsPerWindow = maxCallsPerWindow;
        this.clock = clock;
    }

    /** Compte un appel pour cet utilisateur, ou répond {@code false} s'il a atteint la limite de l'heure en cours. */
    boolean tryAcquire(String user) {
        Instant now = clock.instant();
        if (windows.size() >= MAX_TRACKED_USERS) {
            windows.values().removeIf(window -> expired(window, now));
        }
        boolean[] acquired = new boolean[1];
        windows.compute(user, (key, window) -> {
            if (window == null || expired(window, now)) {
                acquired[0] = maxCallsPerWindow > 0;
                return new Window(now, acquired[0] ? 1 : 0);
            }
            acquired[0] = window.calls() < maxCallsPerWindow;
            return acquired[0] ? new Window(window.start(), window.calls() + 1) : window;
        });
        return acquired[0];
    }

    private static boolean expired(Window window, Instant now) {
        return !now.isBefore(window.start().plus(WINDOW));
    }
}
