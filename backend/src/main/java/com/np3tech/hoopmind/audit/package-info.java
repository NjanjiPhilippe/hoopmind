/**
 * Journal d'audit : qui a fait quoi, sur quoi, et avec quel résultat.
 * <p>
 * Le module écoute les événements que les autres modules publient pour eux-mêmes ({@code UserAccountEvent},
 * {@code AuthenticationEvent}…) : aucun module ne dépend de l'audit. Chaque événement est enregistré une fois
 * la transaction de l'appelant terminée (validée pour une réussite, validée ou annulée pour un échec), écrit
 * dans le journal applicatif {@code hoopmind.audit} et consultable par les administrateurs.
 * Aucune adresse IP, aucun mot de passe ni aucune valeur saisie n'y est conservé.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Audit")
package com.np3tech.hoopmind.audit;
