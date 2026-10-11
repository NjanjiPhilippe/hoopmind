package com.np3tech.hoopmind.ml.scoring;

import java.util.List;

/**
 * Les variables d'entrée du modèle, dans l'ordre où l'entraînement et la prédiction les assemblent. Source unique :
 * le pipeline Spark et le calcul en Java lisent cette liste.
 */
public final class GameOutcomeFeatures {

    public static final List<String> NAMES = List.of(
            "win_pct_diff", "last10_win_pct_diff", "net_rating_diff", "rest_days_diff", "is_playoff");

    private GameOutcomeFeatures() {
    }

    /** Écarts entre l'équipe à domicile et l'équipe extérieure, dans l'ordre de {@link #NAMES}. */
    public static double[] of(TeamForm home, TeamForm away, boolean playoff) {
        return new double[]{
                home.winPct() - away.winPct(),
                home.last10WinPct() - away.last10WinPct(),
                home.netRating() - away.netRating(),
                home.restDays() - away.restDays(),
                playoff ? 1.0 : 0.0
        };
    }
}
