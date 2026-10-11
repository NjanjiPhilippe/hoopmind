package com.np3tech.hoopmind.audit;

/**
 * Actions tracées dans le journal d'audit.
 */
public enum AuditAction {
    USER_REGISTERED,
    LOGIN_SUCCEEDED,
    LOGIN_FAILED,
    LOGOUT,
    REFRESH_TOKEN_REUSED,
    PROFILE_UPDATED,
    PASSWORD_CHANGED,
    PASSWORD_CHANGE_FAILED,
    ADMIN_BOOTSTRAPPED,
    USER_ENABLED,
    USER_DISABLED,
    USER_ROLES_CHANGED,
    USER_DELETED
}
