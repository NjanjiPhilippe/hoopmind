package com.np3tech.hoopmind.ml;

/**
 * Score prédit d'un match, par deux régressions linéaires sur les moyennes de points des équipes.
 *
 * @param homePoints              points prédits pour l'équipe à domicile, arrondis (jamais égaux à ceux de l'autre
 *                                équipe : un match de basket ne finit pas sur un nul)
 * @param awayPoints              points prédits pour l'équipe à l'extérieur, arrondis
 * @param margin                  écart prédit avant arrondi, du point de vue de l'équipe à domicile (négatif : elle
 *                                perd)
 * @param total                   total de points prédit avant arrondi
 * @param marginError             erreur moyenne du modèle sur l'écart, mesurée sur la saison de test (en points)
 * @param homePointsPerGame       points marqués par match par l'équipe à domicile avant ce match (mêlés à la moyenne
 *                                de la saison passée en début de saison), la base de sa prédiction
 * @param homePointsAllowedPerGame points encaissés par match par l'équipe à domicile
 * @param awayPointsPerGame       même chose pour l'équipe à l'extérieur
 * @param awayPointsAllowedPerGame même chose pour l'équipe à l'extérieur
 */
public record ScorePrediction(int homePoints, int awayPoints, double margin, double total, double marginError,
                              double homePointsPerGame, double homePointsAllowedPerGame, double awayPointsPerGame,
                              double awayPointsAllowedPerGame) {

    /** Arrondit les deux scores sans jamais rendre un match nul : l'équipe qui a l'écart en sa faveur garde un point. */
    public static ScorePrediction of(double homePoints, double awayPoints, double marginError, double homePointsPerGame,
                                     double homePointsAllowedPerGame, double awayPointsPerGame,
                                     double awayPointsAllowedPerGame) {
        int home = (int) Math.round(homePoints);
        int away = (int) Math.round(awayPoints);
        if (home == away) {
            if (homePoints >= awayPoints) home++;
            else away++;
        }
        return new ScorePrediction(home, away, homePoints - awayPoints, homePoints + awayPoints, marginError,
                homePointsPerGame, homePointsAllowedPerGame, awayPointsPerGame, awayPointsAllowedPerGame);
    }

    public boolean homeTeamWins() {
        return homePoints > awayPoints;
    }
}
