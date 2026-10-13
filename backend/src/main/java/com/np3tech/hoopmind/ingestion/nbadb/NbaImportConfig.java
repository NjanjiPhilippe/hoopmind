package com.np3tech.hoopmind.ingestion.nbadb;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({NbaDatabaseImportProperties.class, KaggleProperties.class, StatsNbaProperties.class,
        EoinMooreProperties.class})
class NbaImportConfig {

    /**
     * Import au démarrage, pratique pour une démo en conteneurs ({@code NBA_IMPORT_ON_STARTUP=true}). Il passe par
     * le job comme un import lancé depuis la page Données : même verrou, même suivi, et l'API répond pendant l'import.
     */
    @Bean
    @ConditionalOnProperty(name = "hoopmind.import.nba-database.on-startup", havingValue = "true")
    ApplicationRunner importNbaDatabaseOnStartup(NbaDatabaseJobService jobs) {
        return args -> jobs.start(null, false);
    }
}
