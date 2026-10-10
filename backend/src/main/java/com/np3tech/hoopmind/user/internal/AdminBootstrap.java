package com.np3tech.hoopmind.user.internal;

import com.np3tech.hoopmind.user.UserAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Crée le premier administrateur au démarrage à partir des variables d'environnement
 * {@code HOOPMIND_ADMIN_USERNAME}, {@code HOOPMIND_ADMIN_EMAIL} et {@code HOOPMIND_ADMIN_PASSWORD}.
 * Ne fait rien si un administrateur actif existe déjà.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(AdminBootstrap.Properties.class)
@ConditionalOnProperty(prefix = "hoopmind.bootstrap-admin", name = "password")
class AdminBootstrap implements ApplicationRunner {

    private final UserAccountService accounts;
    private final Properties properties;

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(properties.password())) {
            return;
        }
        if (accounts.createAdminIfNoneExists(properties.username(), properties.email(), properties.password())) {
            log.info("Administrateur initial « {} » créé", properties.username());
        }
    }

    @ConfigurationProperties("hoopmind.bootstrap-admin")
    record Properties(String username, String email, String password) {
    }
}
