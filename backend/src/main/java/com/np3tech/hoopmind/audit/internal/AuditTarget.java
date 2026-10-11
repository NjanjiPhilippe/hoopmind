package com.np3tech.hoopmind.audit.internal;

/**
 * Objet concerné par une action. {@code label} garde un nom lisible même après suppression de l'objet.
 */
record AuditTarget(String type, String id, String label) {

    static final String USER = "USER";
    static final String DATASET = "DATASET";
    static final String MODEL = "MODEL";

    static AuditTarget user(Long id, String username) {
        return new AuditTarget(USER, String.valueOf(id), username);
    }
}
