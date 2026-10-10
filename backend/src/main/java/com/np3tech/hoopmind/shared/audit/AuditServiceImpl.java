package com.np3tech.hoopmind.shared.audit;

import com.np3tech.hoopmind.shared.audit.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for auditing and logging important system events
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {

    /**
     * Log user action
     */
    public void logUserAction(Long userId, String username, String action, String details) {
        Map<String, Object> auditLog = new HashMap<>();
        auditLog.put("timestamp", LocalDateTime.now());
        auditLog.put("userId", userId);
        auditLog.put("username", username);
        auditLog.put("action", action);
        auditLog.put("details", details);

        log.info("USER_ACTION: {}", auditLog);

        // TODO: Store in audit table or send to audit logging service
    }

    /**
     * Log authentication event
     */
    public void logAuthenticationEvent(String username, String event, boolean success, String ipAddress) {
        Map<String, Object> auditLog = new HashMap<>();
        auditLog.put("timestamp", LocalDateTime.now());
        auditLog.put("username", username);
        auditLog.put("event", event);
        auditLog.put("success", success);
        auditLog.put("ipAddress", ipAddress);

        log.info("AUTH_EVENT: {}", auditLog);
    }

    /**
     * Log data modification
     */
    public void logDataModification(String entityType, Long entityId, String operation, Long userId) {
        Map<String, Object> auditLog = new HashMap<>();
        auditLog.put("timestamp", LocalDateTime.now());
        auditLog.put("entityType", entityType);
        auditLog.put("entityId", entityId);
        auditLog.put("operation", operation);
        auditLog.put("userId", userId);

        log.info("DATA_MODIFICATION: {}", auditLog);
    }

    /**
     * Log security event
     */
    public void logSecurityEvent(String event, String severity, String details, String ipAddress) {
        Map<String, Object> auditLog = new HashMap<>();
        auditLog.put("timestamp", LocalDateTime.now());
        auditLog.put("event", event);
        auditLog.put("severity", severity);
        auditLog.put("details", details);
        auditLog.put("ipAddress", ipAddress);

        log.warn("SECURITY_EVENT: {}", auditLog);
    }

    /**
     * Log system event
     */
    public void logSystemEvent(String event, String level, String message) {
        Map<String, Object> auditLog = new HashMap<>();
        auditLog.put("timestamp", LocalDateTime.now());
        auditLog.put("event", event);
        auditLog.put("level", level);
        auditLog.put("message", message);

        log.info("SYSTEM_EVENT: {}", auditLog);
    }

    /**
     * Log prediction evaluation
     */
    public void logPredictionEvaluation(Long predictionId, Long userId, boolean correct, Integer points) {
        Map<String, Object> auditLog = new HashMap<>();
        auditLog.put("timestamp", LocalDateTime.now());
        auditLog.put("predictionId", predictionId);
        auditLog.put("userId", userId);
        auditLog.put("correct", correct);
        auditLog.put("pointsAwarded", points);

        log.info("PREDICTION_EVALUATION: {}", auditLog);
    }

    /**
     * Log API request
     */
    public void logApiRequest(String endpoint, String method, String username, int statusCode, long duration) {
        Map<String, Object> auditLog = new HashMap<>();
        auditLog.put("timestamp", LocalDateTime.now());
        auditLog.put("endpoint", endpoint);
        auditLog.put("method", method);
        auditLog.put("username", username);
        auditLog.put("statusCode", statusCode);
        auditLog.put("durationMs", duration);

        log.debug("API_REQUEST: {}", auditLog);
    }

    /**
     * Log error
     */
    public void logError(String errorType, String message, String stackTrace, Long userId) {
        Map<String, Object> auditLog = new HashMap<>();
        auditLog.put("timestamp", LocalDateTime.now());
        auditLog.put("errorType", errorType);
        auditLog.put("message", message);
        auditLog.put("stackTrace", stackTrace);
        auditLog.put("userId", userId);

        log.error("ERROR_LOG: {}", auditLog);
    }
}