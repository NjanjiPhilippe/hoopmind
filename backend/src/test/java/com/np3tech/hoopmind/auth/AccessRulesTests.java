package com.np3tech.hoopmind.auth;

import com.np3tech.hoopmind.IntegrationTest;
import com.np3tech.hoopmind.TestAccounts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Matrice des droits d'accès de l'API, vérifiée route par route.
 * <p>
 * Chaque route de {@code /api} doit correspondre à une ligne de {@link #RULES} (la première qui correspond
 * l'emporte), et chaque ligne doit servir : une nouvelle route oubliée dans la matrice fait échouer le test.
 * On vérifie ensuite que les règles sont réellement appliquées : un anonyme reçoit 401 hors des routes publiques,
 * un simple utilisateur reçoit 403 sur les routes réservées, et l'annotation {@link PreAuthorize} de chaque route
 * correspond à la matrice.
 */
@IntegrationTest
class AccessRulesTests {

    enum Access {
        /** Sans compte. */
        PUBLIC,
        /** Tout compte connecté. */
        USER,
        /** Administrateur seulement. */
        ADMIN
    }

    /** Méthode HTTP (ou *), motif de chemin, accès attendu. */
    private static final String RULES = """
            POST   /api/v1/auth/**                          PUBLIC
            *      /api/v1/admin/**                         ADMIN
            GET    /api/v1/users/me                         USER
            PATCH  /api/v1/users/me                         USER
            PUT    /api/v1/users/me/password                USER
            GET    /api/v1/games/**                         PUBLIC
            GET    /api/v1/teams/**                         PUBLIC
            GET    /api/v1/players/**                       PUBLIC
            GET    /api/v1/seasons/**                       PUBLIC
            GET    /api/v1/stats/**                         PUBLIC
            *      /api/v1/games/**                         ADMIN
            *      /api/v1/teams/**                         ADMIN
            *      /api/v1/players/**                       ADMIN
            *      /api/v1/seasons/**                       ADMIN
            *      /api/v1/predictions/mine/**              USER
            *      /api/v1/spark/ml/train/**                ADMIN
            GET    /api/v1/spark/ml/**                      USER
            """;

    @Autowired
    MockMvcTester mvc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mappings;

    String userToken;

    @BeforeEach
    void signIn() {
        userToken = TestAccounts.register(mvc).accessToken();
    }

    @Test
    void everyRouteHasExactlyOneRuleAndEveryRuleIsUsed() {
        List<Rule> rules = rules();
        Set<Rule> used = new HashSet<>();
        for (Route route : routes()) {
            used.add(rules.stream().filter(rule -> rule.matches(route)).findFirst()
                    .orElseThrow(() -> new AssertionError("Route absente de la matrice : " + route)));
        }
        assertThat(rules).as("lignes de la matrice qui ne correspondent à aucune route").containsExactlyInAnyOrderElementsOf(used);
    }

    /**
     * Le refus de Spring Security se reconnaît à l'en-tête {@code WWW-Authenticate} : une route publique peut
     * répondre 401 d'elle-même (rafraîchissement sans cookie), mais jamais par ce refus.
     */
    @Test
    void anonymousVisitorsOnlyReachPublicRoutes() {
        for (Route route : routes()) {
            MockHttpServletResponse response = mvc.perform(request(route.method(), route.samplePath())).getResponse();
            if (access(route) == Access.PUBLIC) {
                assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).as("%s, sans compte", route).isNull();
                assertThat(response.getStatus()).as("%s, sans compte", route).isNotEqualTo(403);
            } else {
                assertThat(response.getStatus()).as("%s, sans compte", route).isEqualTo(401);
                assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).as("%s, sans compte", route).isEqualTo("Bearer");
            }
        }
    }

    /**
     * Seulement pour les routes sans corps ni paramètre obligatoire : sinon la validation (400) passe avant le
     * contrôle des droits. Ces routes-là sont couvertes par {@link #annotationsMatchTheMatrix()}.
     */
    @Test
    void regularUsersAreRefusedOnRestrictedRoutes() {
        for (Route route : routes()) {
            if (access(route) == Access.ADMIN && !route.needsInput()) {
                int status = mvc.perform(request(route.method(), route.samplePath())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                        .getResponse().getStatus();
                assertThat(status).as("%s, simple utilisateur", route).isEqualTo(403);
            }
        }
    }

    @Test
    void annotationsMatchTheMatrix() {
        for (Route route : routes()) {
            String guard = route.guard();
            switch (access(route)) {
                case PUBLIC, USER -> assertThat(guard).as("%s ne doit pas exiger de rôle", route).isNull();
                case ADMIN -> assertThat(guard).as(route.toString()).isEqualTo("hasRole('ADMIN')");
            }
        }
    }

    private Access access(Route route) {
        return rules().stream().filter(rule -> rule.matches(route)).findFirst().orElseThrow().access();
    }

    private List<Route> routes() {
        List<Route> routes = new ArrayList<>();
        mappings.getHandlerMethods().forEach((info, handler) -> {
            if (info.getPathPatternsCondition() == null) {
                return;
            }
            for (String path : info.getPathPatternsCondition().getPatternValues()) {
                if (!path.startsWith("/api/")) {
                    continue;
                }
                info.getMethodsCondition().getMethods().forEach(method ->
                        routes.add(new Route(HttpMethod.valueOf(method.name()), path, handler)));
            }
        });
        assertThat(routes).isNotEmpty();
        return routes;
    }

    private static List<Rule> rules() {
        return RULES.lines().map(String::trim).filter(line -> !line.isEmpty()).map(line -> {
            String[] parts = line.split("\\s+");
            return new Rule(parts[0], PathPatternParser.defaultInstance.parse(parts[1]), Access.valueOf(parts[2]));
        }).toList();
    }

    private record Rule(String method, PathPattern pattern, Access access) {

        boolean matches(Route route) {
            return (method.equals("*") || method.equals(route.method().name()))
                    && pattern.matches(PathContainer.parsePath(route.samplePath()));
        }

        @Override
        public String toString() {
            return method + " " + pattern.getPatternString() + " " + access;
        }
    }

    private record Route(HttpMethod method, String path, HandlerMethod handler) {

        /** Le chemin avec 1 à la place de chaque variable. */
        String samplePath() {
            return path.replaceAll("\\{[^}]+}", "1");
        }

        /** Corps de requête ou paramètre obligatoire : la validation répondrait 400 avant le contrôle des droits. */
        boolean needsInput() {
            return Arrays.stream(handler.getMethodParameters()).anyMatch(parameter ->
                    parameter.hasParameterAnnotation(RequestBody.class)
                            || (parameter.hasParameterAnnotation(RequestParam.class)
                            && parameter.getParameterAnnotation(RequestParam.class).required()
                            && parameter.getParameterAnnotation(RequestParam.class).defaultValue()
                            .equals(ValueConstants.DEFAULT_NONE)));
        }

        /** Expression {@link PreAuthorize} de la méthode, sinon de la classe, sinon {@code null}. */
        String guard() {
            PreAuthorize onMethod = handler.getMethodAnnotation(PreAuthorize.class);
            if (onMethod != null) {
                return onMethod.value();
            }
            PreAuthorize onClass = handler.getBeanType().getAnnotation(PreAuthorize.class);
            return onClass == null ? null : onClass.value();
        }

        @Override
        public String toString() {
            return method + " " + path;
        }
    }
}
