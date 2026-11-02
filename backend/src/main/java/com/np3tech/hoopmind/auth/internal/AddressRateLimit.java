package com.np3tech.hoopmind.auth.internal;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Nombre d'opérations permises à une adresse IP par fenêtre fixe (en mémoire, propre à l'instance). Sert aux
 * opérations qui créent quelque chose sans mot de passe à deviner : inscription et session de démonstration.
 */
final class AddressRateLimit {

    /** Au-delà, les fenêtres terminées sont oubliées ; s'il n'y en a pas, une nouvelle adresse est refusée. */
    private static final int MAX_TRACKED_ADDRESSES = 100_000;

    private record Window(Instant start, int count) {
    }

    private final int maxPerWindow;
    private final Duration window;
    private final Clock clock;
    private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();

    AddressRateLimit(int maxPerWindow, Duration window, Clock clock) {
        this.maxPerWindow = maxPerWindow;
        this.window = window;
        this.clock = clock;
    }

    /** Compte une opération pour cette adresse, ou renvoie le délai d'attente si la limite est atteinte. */
    Optional<Duration> tryAcquire(String address) {
        Instant now = clock.instant();
        String key = address == null ? "unknown" : address;
        if (windows.size() >= MAX_TRACKED_ADDRESSES) {
            windows.values().removeIf(w -> expired(w, now));
            if (windows.size() >= MAX_TRACKED_ADDRESSES && !windows.containsKey(key)) {
                return Optional.of(window);
            }
        }
        Duration[] retryAfter = new Duration[1];
        windows.compute(key, (k, current) -> {
            if (current == null || expired(current, now)) {
                return new Window(now, 1);
            }
            if (current.count() >= maxPerWindow) {
                retryAfter[0] = Duration.between(now, current.start().plus(window));
                return current;
            }
            return new Window(current.start(), current.count() + 1);
        });
        return Optional.ofNullable(retryAfter[0]);
    }

    private boolean expired(Window w, Instant now) {
        return !now.isBefore(w.start().plus(window));
    }
}
