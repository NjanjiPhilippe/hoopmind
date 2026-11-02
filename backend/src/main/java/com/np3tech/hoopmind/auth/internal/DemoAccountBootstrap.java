package com.np3tech.hoopmind.auth.internal;

import com.np3tech.hoopmind.auth.config.DemoProperties;
import com.np3tech.hoopmind.user.UserAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Crée le compte de démonstration au démarrage quand {@code DEMO_ENABLED=true} : les premières connexions de
 * démonstration simultanées ne se disputent pas sa création.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "hoopmind.demo", name = "enabled", havingValue = "true")
class DemoAccountBootstrap implements ApplicationRunner {

    private final UserAccountService accounts;
    private final DemoProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        accounts.demoAccount(properties.username(), properties.email());
        log.info("Démonstration activée sur le compte « {} » (lecture seule)", properties.username());
    }
}
