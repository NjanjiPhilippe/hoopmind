package com.np3tech.hoopmind.shared.audit;

public interface AuditService {
    void logUserAction(Long userId, String username, String action, String details);
    void logAuthenticationEvent(String username, String event, boolean success, String ipAddress);
    void logDataModification(String entityType, Long entityId, String operation, Long userId);
    void logSecurityEvent(String event, String severity, String details, String ipAddress);
    void logSystemEvent(String event, String level, String message);
    void logPredictionEvaluation(Long predictionId, Long userId, boolean correct, Integer points);
    void logApiRequest(String endpoint, String method, String username, int statusCode, long duration);
    void logError(String errorType, String message, String stackTrace, Long userId);
}
