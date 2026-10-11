package com.np3tech.hoopmind.nba.importer;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({NbaDatabaseImportProperties.class, KaggleProperties.class})
class NbaImportConfig {

    /** Import au démarrage, pratique pour une démo en conteneurs ({@code NBA_IMPORT_ON_STARTUP=true}). */
    @Bean
    @ConditionalOnProperty(name = "hoopmind.import.nba-database.on-startup", havingValue = "true")
    ApplicationRunner importNbaDatabaseOnStartup(NbaDatabaseImporter importer) {
        return args -> importer.importDatabase(null);
    }
}
