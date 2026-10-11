package com.np3tech.hoopmind.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

/** Active {@code @Retryable} et {@code @ConcurrencyLimit} de Spring Framework 7 (sans dépendance supplémentaire). */
@Configuration(proxyBeanMethods = false)
@EnableResilientMethods
public class ResilienceConfig {
}
