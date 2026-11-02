package com.np3tech.hoopmind.auth.config;

import com.np3tech.hoopmind.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Un motif public sans route derrière (route retirée) ouvrirait sans le dire toute route ajoutée plus tard sous ce
 * préfixe, par exemple les blessures de la version privée.
 */
@IntegrationTest
class PublicReadPathsTests {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mappings;

    @Test
    void everyPublicReadPathLeadsToAnExistingRoute() {
        Set<String> routes = mappings.getHandlerMethods().keySet().stream()
                .flatMap(info -> info.getPathPatternsCondition().getPatternValues().stream())
                .collect(Collectors.toSet());

        assertThat(Arrays.stream(SecurityConfig.PUBLIC_READ_PATHS).map(path -> path.replace("/**", "")))
                .allSatisfy(prefix -> assertThat(routes).anyMatch(route -> route.startsWith(prefix)));
    }
}
