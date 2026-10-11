package com.np3tech.hoopmind.audit;

/**
 * Objet concerné par une action. {@code label} garde un nom lisible même après suppression de l'objet.
 */
public record AuditTarget(String type, String id, String label) {

    public static final String USER = "USER";

    public static AuditTarget user(Long id, String username) {
        return new AuditTarget(USER, String.valueOf(id), username);
    }
}
