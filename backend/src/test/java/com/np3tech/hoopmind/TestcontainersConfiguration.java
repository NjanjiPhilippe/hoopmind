package com.np3tech.hoopmind;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * PostgreSQL jetable pour les tests d'intégration. Pour utiliser une base existante à la place
 * (machine sans Docker), lancer avec {@code -Dhoopmind.test.testcontainers=false} et
 * {@code -Dspring.datasource.url=... -Dspring.datasource.username=... -Dspring.datasource.password=...}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    @ConditionalOnProperty(name = "hoopmind.test.testcontainers", havingValue = "true", matchIfMissing = true)
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));
    }
}
