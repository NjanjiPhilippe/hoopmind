package com.np3tech.hoopmind.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

/**
 * Active {@code @Retryable} et {@code @ConcurrencyLimit} de Spring Framework 7 (sans dépendance supplémentaire).
 * <p>
 * Proxys de classe : sur un proxy d'interface, {@code @ConcurrencyLimit} posé sur la méthode de l'implémentation
 * n'est pas trouvé (« No @ConcurrencyLimit annotation found ») ; c'est le cas des adaptateurs LLM.
 */
@Configuration(proxyBeanMethods = false)
@EnableResilientMethods(proxyTargetClass = true)
public class ResilienceConfig {
}
