/**
 * Journal d'audit : qui a fait quoi, sur quoi, et avec quel résultat.
 * <p>
 * Les autres modules publient un {@link com.np3tech.hoopmind.audit.AuditEvent} ; le module l'enregistre
 * une fois la transaction de l'appelant terminée (validée pour une réussite, validée ou annulée pour un échec),
 * l'écrit dans le journal applicatif {@code hoopmind.audit} et le rend consultable par les administrateurs.
 * Aucune adresse IP, aucun mot de passe ni aucune valeur saisie n'y est conservé.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Audit")
package com.np3tech.hoopmind.audit;
