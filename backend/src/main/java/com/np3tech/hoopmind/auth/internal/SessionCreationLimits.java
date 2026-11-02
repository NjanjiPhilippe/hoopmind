package com.np3tech.hoopmind.auth.internal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

/**
 * Limites par adresse IP et par heure des opérations qui ouvrent une session sans mot de passe à deviner :
 * l'inscription (sans elle, un script créerait des comptes pour multiplier le quota LLM de chaque utilisateur, ou
 * pour savoir quelles adresses e-mail ont déjà un compte) et la session de démonstration.
 */
@Component
class SessionCreationLimits {

    private final AddressRateLimit registrations;
    private final AddressRateLimit demoSessions;

    SessionCreationLimits(@Value("${hoopmind.security.registrations-per-address-per-hour:5}") int registrations,
                          @Value("${hoopmind.security.demo-sessions-per-address-per-hour:30}") int demoSessions,
                          Clock clock) {
        this.registrations = new AddressRateLimit(registrations, Duration.ofHours(1), clock);
        this.demoSessions = new AddressRateLimit(demoSessions, Duration.ofHours(1), clock);
    }

    void registration(String address) {
        registrations.tryAcquire(address).ifPresent(retryAfter -> {
            throw new LoginThrottledException(retryAfter, "Trop de comptes créés depuis cette adresse.");
        });
    }

    void demoSession(String address) {
        demoSessions.tryAcquire(address).ifPresent(retryAfter -> {
            throw new LoginThrottledException(retryAfter, "Trop de sessions de démonstration ouvertes depuis cette adresse.");
        });
    }
}
