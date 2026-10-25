package com.np3tech.hoopmind.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Identifiants HTTP Basic de Prometheus pour lire {@code /actuator/prometheus}.
 *
 * @param password sans mot de passe, l'endpoint reste fermé
 */
@ConfigurationProperties("hoopmind.monitoring.scrape")
record MetricsScrapeProperties(@DefaultValue("prometheus") String username, String password) {

    boolean enabled() {
        return password != null && !password.isBlank();
    }

    /** Comparaison en temps constant, pour ne rien laisser deviner du mot de passe. */
    boolean matches(String candidateUsername, String candidatePassword) {
        if (!enabled() || candidateUsername == null || candidatePassword == null) {
            return false;
        }
        boolean user = MessageDigest.isEqual(bytes(username), bytes(candidateUsername));
        boolean pass = MessageDigest.isEqual(bytes(password), bytes(candidatePassword));
        return user & pass;
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
