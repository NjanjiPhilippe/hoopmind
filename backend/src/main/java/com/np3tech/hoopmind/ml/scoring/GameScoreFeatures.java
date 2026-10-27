package com.np3tech.hoopmind.ml.scoring;

import java.util.List;

/**
 * Les variables d'entrée du modèle de score, dans l'ordre où l'entraînement et la prédiction les assemblent. Contrairement
 * au modèle du vainqueur, qui ne voit que des écarts, le score dépend du niveau absolu des deux équipes : deux attaques
 * à 120 points ne donnent pas le même total que deux attaques à 100. Chaque équipe garde donc ses moyennes.
 */
public final class GameScoreFeatures {

    public static final List<String> NAMES = List.of(
            "home_points_per_game", "home_points_allowed_per_game", "away_points_per_game",
            "away_points_allowed_per_game", "rest_days_diff", "is_playoff");

    private GameScoreFeatures() {
    }

    /** Valeurs dans l'ordre de {@link #NAMES}. */
    public static double[] of(TeamForm home, TeamForm away, boolean playoff) {
        return new double[]{
                home.pointsPerGame(),
                home.pointsAllowedPerGame(),
                away.pointsPerGame(),
                away.pointsAllowedPerGame(),
                home.restDays() - away.restDays(),
                playoff ? 1.0 : 0.0
        };
    }
}
