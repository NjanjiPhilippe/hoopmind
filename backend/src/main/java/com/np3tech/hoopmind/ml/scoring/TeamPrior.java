package com.np3tech.hoopmind.ml.scoring;

import com.np3tech.hoopmind.nba.GameRecord;
import com.np3tech.hoopmind.nba.GameRecord.BoxScore;

import java.util.List;

/**
 * Bilan d'une équipe sur la saison précédente, tous matchs joués compris (playoffs aussi) : le point de départ de sa
 * forme en début de saison. Même calcul que {@code GameFeatureBuilder} côté Spark.
 *
 * @param gamesPlayed matchs joués la saison passée ; 0 si l'équipe n'y apparaît pas (valeurs neutres)
 */
public record TeamPrior(long gamesPlayed, double winPct, double offensiveRating, double defensiveRating) {

    public static final TeamPrior NONE =
            new TeamPrior(0, TeamForm.NEUTRAL_WIN_PCT, TeamForm.NEUTRAL_RATING, TeamForm.NEUTRAL_RATING);

    /** @param previousSeason matchs de la saison précédente (au moins ceux de l'équipe) */
    public static TeamPrior of(List<GameRecord> previousSeason, long teamId) {
        long played = 0;
        long wins = 0;
        Double scored = null;
        Double allowed = null;
        double ownPossessions = 0;
        double opponentPossessions = 0;
        for (GameRecord game : previousSeason) {
            boolean home = game.homeTeamId() == teamId;
            if (!home && game.awayTeamId() != teamId || !game.played()) {
                continue;
            }
            int points = home ? game.homePoints() : game.awayPoints();
            int pointsAllowed = home ? game.awayPoints() : game.homePoints();
            played++;
            wins += points > pointsAllowed ? 1 : 0;
            BoxScore own = home ? game.home() : game.away();
            BoxScore opponent = home ? game.away() : game.home();
            if (own != null && opponent != null) {
                ownPossessions += own.possessions();
                opponentPossessions += opponent.possessions();
                scored = (scored == null ? 0 : scored) + points;
                allowed = (allowed == null ? 0 : allowed) + pointsAllowed;
            }
        }
        if (played == 0) {
            return NONE;
        }
        Double offensive = TeamForm.rating(scored, ownPossessions);
        Double defensive = TeamForm.rating(allowed, opponentPossessions);
        return new TeamPrior(played, (double) wins / played,
                offensive == null ? TeamForm.NEUTRAL_RATING : offensive,
                defensive == null ? TeamForm.NEUTRAL_RATING : defensive);
    }
}
