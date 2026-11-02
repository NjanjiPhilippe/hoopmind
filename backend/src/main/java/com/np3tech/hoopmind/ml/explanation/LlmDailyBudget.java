package com.np3tech.hoopmind.ml.explanation;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Nombre d'appels au LLM permis par jour (UTC), tous utilisateurs confondus.
 * <p>
 * La limite par utilisateur ({@link LlmCallQuota}) ne suffit pas : l'inscription est ouverte, et plusieurs comptes
 * créés par un script videraient le quota gratuit du fournisseur pour la journée. Au-delà de ce plafond, tout le
 * monde reçoit l'explication rédigée sans LLM. L'état est en mémoire, propre à l'instance.
 */
final class LlmDailyBudget {

    private final int maxCallsPerDay;
    private final Clock clock;
    private LocalDate day;
    private int calls;

    LlmDailyBudget(int maxCallsPerDay, Clock clock) {
        this.maxCallsPerDay = maxCallsPerDay;
        this.clock = clock;
    }

    /** Compte un appel, ou répond {@code false} si le plafond du jour est atteint. */
    synchronized boolean tryAcquire() {
        LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
        if (!today.equals(day)) {
            day = today;
            calls = 0;
        }
        if (calls >= maxCallsPerDay) {
            return false;
        }
        calls++;
        return true;
    }
}
