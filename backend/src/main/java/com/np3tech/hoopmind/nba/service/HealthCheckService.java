package com.np3tech.hoopmind.nba.service;

import java.util.Map;

public interface HealthCheckService {
    Map<String, Object> getSystemHealth();
    Map<String, Object> getDatabaseMetrics();
    boolean isSystemReady();
}
