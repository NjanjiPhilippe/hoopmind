package com.np3tech.hoopmind.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentation OpenAPI. Swagger UI est réservé aux administrateurs (voir la configuration de sécurité)
 * et désactivé en profil {@code prod}.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI hoopMindOpenApi(@Value("${hoopmind.version:dev}") String version) {
        return new OpenAPI()
                .info(new Info()
                        .title("HoopMind API")
                        .version(version)
                        .description("Statistiques et prédictions NBA. Obtenez un jeton avec POST /api/v1/auth/login, "
                                + "puis utilisez le bouton Authorize.")
                        .license(new License().name("Tous droits réservés")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
