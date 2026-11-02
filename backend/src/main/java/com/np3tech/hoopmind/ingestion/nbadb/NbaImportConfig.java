package com.np3tech.hoopmind.ingestion.nbadb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({NbaDatabaseImportProperties.class, KaggleProperties.class, EoinMooreProperties.class})
class NbaImportConfig {

    /**
     * Import au démarrage, pratique pour une démo en conteneurs ({@code NBA_IMPORT_ON_STARTUP=true}). Il passe par
     * le job comme un import lancé depuis la page Données : même verrou, même suivi, et l'API répond pendant l'import.
     * Un import impossible (fichier absent, autre opération en cours) est journalisé : il ne doit pas empêcher l'API
     * de démarrer.
     */
    @Bean
    @ConditionalOnProperty(name = "hoopmind.import.nba-database.on-startup", havingValue = "true")
    ApplicationRunner importNbaDatabaseOnStartup(NbaDatabaseJobService jobs) {
        return args -> {
            try {
                jobs.start(null, false);
            } catch (RuntimeException e) {
                log.warn("Import au démarrage non lancé : {}", e.getMessage());
            }
        };
    }
}
