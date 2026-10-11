package com.np3tech.hoopmind.audit.internal;

/**
 * Auteur d'une action : un utilisateur (identifiant et nom au moment de l'action) ou le système.
 */
record AuditActor(Long id, String login) {

    static final AuditActor SYSTEM = new AuditActor(null, "système");

    static AuditActor user(Long id, String login) {
        return new AuditActor(id, login);
    }
}
