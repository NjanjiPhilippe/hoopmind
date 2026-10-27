/**
 * Données NBA : équipes, joueurs, saisons, matchs et statistiques.
 * <p>
 * Module ouvert : l'import ({@code ingestion}) écrit directement ses entités et dépôts. Le module ML, lui, ne passe
 * que par {@link com.np3tech.hoopmind.nba.GameHistory}.
 */
@org.springframework.modulith.ApplicationModule(displayName = "NBA", type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package com.np3tech.hoopmind.nba;
