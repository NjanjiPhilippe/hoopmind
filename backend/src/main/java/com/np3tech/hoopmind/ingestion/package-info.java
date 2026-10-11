/**
 * Ingestion des données NBA : téléchargement Kaggle, import de la base SQLite (Walsh) et import des CSV de
 * référence, avec leurs jobs et leurs endpoints d'administration.
 * <p>
 * Module fermé : aucun autre module ne l'appelle. Il écrit dans les tables du module {@code nba}
 * (import en masse par JDBC) et s'appuie sur ses entités de référence (équipes, saisons, énumérations).
 */
@org.springframework.modulith.ApplicationModule(displayName = "Ingestion des données")
package com.np3tech.hoopmind.ingestion;
