package com.np3tech.hoopmind.nba;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Historique des matchs, en lecture seule, pour les modules qui en ont besoin (le modèle de prédiction).
 * <p>
 * C'est la seule porte d'entrée du module ML vers les données NBA : il ne connaît ni les tables ni les entités.
 */
public interface GameHistory {

    /** Les {@code count} saisons les plus récentes qui ont des matchs joués, de la plus ancienne à la plus récente. */
    List<Integer> recentSeasonYears(int count);

    /** Matchs de saison régulière et de playoffs des saisons demandées (année de début : 2024 pour 2024-25). */
    List<GameRecord> games(Collection<Integer> seasonYears);

    /** Matchs d'une saison auxquels participe au moins une des équipes (pour calculer leur forme). */
    List<GameRecord> teamGames(int seasonYear, Collection<Long> teamIds);

    /** Année de début d'une saison, d'après son identifiant. */
    Optional<Integer> seasonYear(long seasonId);
}
