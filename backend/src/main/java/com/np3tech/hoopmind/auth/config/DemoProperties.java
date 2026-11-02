package com.np3tech.hoopmind.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Compte de démonstration ({@code hoopmind.demo.*}) : un visiteur ouvre une session sans s'inscrire, sur un compte
 * partagé en lecture seule (voir l'ADR 0027).
 *
 * @param enabled  {@code DEMO_ENABLED}, désactivé par défaut
 * @param username nom du compte partagé, créé au démarrage s'il n'existe pas
 * @param email    adresse du compte (jamais utilisée pour écrire)
 */
@ConfigurationProperties("hoopmind.demo")
public record DemoProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("demo") String username,
        @DefaultValue("demo@hoopmind.invalid") String email
) {
}
