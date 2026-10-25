package com.np3tech.hoopmind.ml.scoring;

import com.np3tech.hoopmind.nba.GameRecord;
import com.np3tech.hoopmind.nba.GameRecord.BoxScore;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/**
 * Forme d'une équipe avant un match, calculée exactement comme {@code GameFeatureBuilder} le fait avec Spark
 * pour l'entraînement : les matchs <b>précédents de la même saison</b>, complétés par le bilan de la saison passée
 * ({@link TeamPrior}) tant que l'équipe a peu joué. Un test compare les deux calculs match par match.
 * <p>
 * Le bilan de la saison passée compte comme {@value #PRIOR_GAMES} matchs : après 10 matchs joués, il pèse encore
 * pour moitié, après 30 pour un quart. Sur les 10 derniers matchs, les places vides (moins de 10 matchs joués)
 * prennent le pourcentage de victoires de la saison passée.
 */
public record TeamForm(long gamesPlayed, double winPct, double last10WinPct, double offensiveRating,
                       double defensiveRating, double restDays) {

    /** Poids du bilan de la saison passée, en nombre de matchs. */
    public static final int PRIOR_GAMES = 10;
    /** Valeurs neutres quand une équipe n'a joué ni cette saison ni la précédente. */
    public static final double NEUTRAL_WIN_PCT = 0.5;
    public static final double NEUTRAL_RATING = 110.0;
    static final int MAX_REST_DAYS = 7;
    private static final int LAST_GAMES = 10;

    private static final Comparator<GameRecord> CHRONOLOGICAL =
            Comparator.comparing(GameRecord::date).thenComparingLong(GameRecord::gameId);

    public double netRating() {
        return offensiveRating - defensiveRating;
    }

    /** Forme sans saison passée connue (valeurs neutres comme point de départ). */
    public static TeamForm before(List<GameRecord> seasonGames, long teamId, LocalDate date, Long gameId) {
        return before(seasonGames, teamId, date, gameId, TeamPrior.NONE);
    }

    /**
     * @param seasonGames matchs de la saison (au moins ceux de l'équipe), dans n'importe quel ordre
     * @param date        date du match à prédire
     * @param gameId      identifiant du match s'il existe déjà (départage deux matchs le même jour), sinon {@code null}
     * @param prior       bilan de l'équipe sur la saison précédente
     */
    public static TeamForm before(List<GameRecord> seasonGames, long teamId, LocalDate date, Long gameId,
                                  TeamPrior prior) {
        List<GameRecord> earlier = seasonGames.stream()
                .filter(game -> game.homeTeamId() == teamId || game.awayTeamId() == teamId)
                .filter(game -> game.date().isBefore(date)
                        || game.date().equals(date) && gameId != null && game.gameId() < gameId)
                .sorted(CHRONOLOGICAL)
                .toList();

        long played = 0;
        long wins = 0;
        long boxScoreGames = 0;
        Double scoredWithBoxScore = null;
        double ownPossessions = 0;
        Double allowedWithBoxScore = null;
        double opponentPossessions = 0;
        for (GameRecord game : earlier) {
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
                boxScoreGames++;
                ownPossessions += own.possessions();
                opponentPossessions += opponent.possessions();
                if (scored != null) scoredWithBoxScore = (scoredWithBoxScore == null ? 0 : scoredWithBoxScore) + scored;
                if (allowed != null) allowedWithBoxScore = (allowedWithBoxScore == null ? 0 : allowedWithBoxScore) + allowed;
            }
        }

        List<GameRecord> lastTen = earlier.subList(Math.max(0, earlier.size() - LAST_GAMES), earlier.size());
        long lastTenPlayed = lastTen.stream().filter(GameRecord::played).count();
        long lastTenWins = lastTen.stream().filter(GameRecord::played).filter(game -> won(game, teamId)).count();

        double restDays = earlier.isEmpty() ? MAX_REST_DAYS
                : Math.min(ChronoUnit.DAYS.between(earlier.getLast().date(), date), MAX_REST_DAYS);

        return new TeamForm(played,
                blendWinPct(wins, played, prior.winPct()),
                blendLastTen(lastTenWins, lastTenPlayed, prior.winPct()),
                blendRating(rating(scoredWithBoxScore, ownPossessions), boxScoreGames, prior.offensiveRating()),
                blendRating(rating(allowedWithBoxScore, opponentPossessions), boxScoreGames, prior.defensiveRating()),
                restDays);
    }

    /** (victoires + 10 × % de la saison passée) / (matchs joués + 10). */
    static double blendWinPct(long wins, long played, double priorWinPct) {
        return (wins + PRIOR_GAMES * priorWinPct) / (played + PRIOR_GAMES);
    }

    /** Les places vides parmi les 10 derniers matchs prennent le % de victoires de la saison passée. */
    static double blendLastTen(long wins, long played, double priorWinPct) {
        return (wins + (LAST_GAMES - played) * priorWinPct) / LAST_GAMES;
    }

    /** Moyenne pondérée : chaque match avec feuille de match compte pour 1, la saison passée pour 10. */
    static double blendRating(Double rating, long games, double priorRating) {
        return rating == null ? priorRating : (rating * games + priorRating * PRIOR_GAMES) / (games + PRIOR_GAMES);
    }

    private static boolean won(GameRecord game, long teamId) {
        boolean home = game.homeTeamId() == teamId;
        return home ? game.homePoints() > game.awayPoints() : game.awayPoints() > game.homePoints();
    }

    /** Points pour 100 possessions, {@code null} sans feuille de match. */
    static Double rating(Double points, double possessions) {
        return points == null || possessions <= 0 ? null : points * 100.0 / possessions;
    }
}
