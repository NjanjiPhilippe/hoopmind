package com.np3tech.hoopmind.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Active {@code @Async}. L'exécuteur est celui de Spring Boot ({@code applicationTaskExecutor}) : avec
 * {@code spring.threads.virtual.enabled}, un fil virtuel par tâche, et un arrêt qui attend les tâches en cours
 * ({@code spring.task.execution.shutdown.*}).
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
public class AsyncConfig {
}
