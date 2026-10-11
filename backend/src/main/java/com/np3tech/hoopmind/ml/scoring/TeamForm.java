package com.np3tech.hoopmind.ml.scoring;

import com.np3tech.hoopmind.nba.GameRecord;
import com.np3tech.hoopmind.nba.GameRecord.BoxScore;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/**
 * Forme d'une équipe avant un match, calculée exactement comme {@code GameFeatureBuilder} le fait avec Spark
 * pour l'entraînement : seuls les matchs <b>précédents de la même saison</b> comptent. Un test compare les deux
 * calculs match par match.
 */
public record TeamForm(long gamesPlayed, double winPct, double last10WinPct, double offensiveRating,
                       double defensiveRating, double restDays) {

    /** Valeurs neutres quand une équipe n'a pas encore joué (début de saison). */
    static final double NEUTRAL_WIN_PCT = 0.5;
    static final double NEUTRAL_RATING = 110.0;
    static final int MAX_REST_DAYS = 7;

    private static final Comparator<GameRecord> CHRONOLOGICAL =
            Comparator.comparing(GameRecord::date).thenComparingLong(GameRecord::gameId);

    public double netRating() {
        return offensiveRating - defensiveRating;
    }

    /**
     * @param seasonGames matchs de la saison (au moins ceux de l'équipe), dans n'importe quel ordre
     * @param date        date du match à prédire
     * @param gameId      identifiant du match s'il existe déjà (départage deux matchs le même jour), sinon {@code null}
     */
    public static TeamForm before(List<GameRecord> seasonGames, long teamId, LocalDate date, Long gameId) {
        List<GameRecord> prior = seasonGames.stream()
                .filter(game -> game.homeTeamId() == teamId || game.awayTeamId() == teamId)
                .filter(game -> game.date().isBefore(date)
                        || game.date().equals(date) && gameId != null && game.gameId() < gameId)
                .sorted(CHRONOLOGICAL)
                .toList();

        long played = 0;
        long wins = 0;
        Double scoredWithBoxScore = null;
        double ownPossessions = 0;
        Double allowedWithBoxScore = null;
        double opponentPossessions = 0;
        for (GameRecord game : prior) {
            boolean home = game.homeTeamId() == teamId;
            Integer scored = home ? game.homePoints() : game.awayPoints();
            Integer allowed = home ? game.awayPoints() : game.homePoints();
            BoxScore own = home ? game.home() : game.away();
            BoxScore opponent = home ? game.away() : game.home();
            if (game.played()) {
                played++;
                wins += scored > allowed ? 1 : 0;
            }
            // Comme à l'entraînement : les possessions ne comptent que si les deux feuilles de match existent.
            if (own != null && opponent != null) {
                ownPossessions += own.possessions();
                opponentPossessions += opponent.possessions();
                if (scored != null) scoredWithBoxScore = (scoredWithBoxScore == null ? 0 : scoredWithBoxScore) + scored;
                if (allowed != null) allowedWithBoxScore = (allowedWithBoxScore == null ? 0 : allowedWithBoxScore) + allowed;
            }
        }

        List<GameRecord> lastTen = prior.subList(Math.max(0, prior.size() - 10), prior.size());
        long lastTenPlayed = lastTen.stream().filter(GameRecord::played).count();
        long lastTenWins = lastTen.stream().filter(GameRecord::played).filter(game -> won(game, teamId)).count();

        double restDays = prior.isEmpty() ? MAX_REST_DAYS
                : Math.min(ChronoUnit.DAYS.between(prior.getLast().date(), date), MAX_REST_DAYS);

        return new TeamForm(played,
                played == 0 ? NEUTRAL_WIN_PCT : (double) wins / played,
                lastTenPlayed == 0 ? NEUTRAL_WIN_PCT : (double) lastTenWins / lastTenPlayed,
                rating(scoredWithBoxScore, ownPossessions),
                rating(allowedWithBoxScore, opponentPossessions),
                restDays);
    }

    private static boolean won(GameRecord game, long teamId) {
        boolean home = game.homeTeamId() == teamId;
        return home ? game.homePoints() > game.awayPoints() : game.awayPoints() > game.homePoints();
    }

    /** Points pour 100 possessions. */
    private static double rating(Double points, double possessions) {
        return points == null || possessions <= 0 ? NEUTRAL_RATING : points * 100.0 / possessions;
    }
}
