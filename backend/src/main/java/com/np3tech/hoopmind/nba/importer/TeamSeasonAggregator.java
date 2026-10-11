package com.np3tech.hoopmind.nba.importer;

import com.np3tech.hoopmind.nba.importer.NbaDatabaseRows.GameRow;
import com.np3tech.hoopmind.nba.importer.NbaDatabaseRows.TeamGameRow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calcule le bilan et les statistiques de saison régulière d'une équipe à partir de ses matchs joués
 * (table {@code team_stats}). Les possessions sont estimées par la formule usuelle
 * {@code FGA - OREB + TOV + 0,44 × FTA}, ce qui donne les ratings offensif et défensif (points pour 100 possessions).
 */
final class TeamSeasonAggregator {

    private TeamSeasonAggregator() {
    }

    record TeamSeason(long teamId, int wins, int losses, int homeWins, int homeLosses, int awayWins, int awayLosses,
                      BigDecimal winPercentage, BigDecimal pointsPerGame, BigDecimal pointsAllowedPerGame,
                      BigDecimal pointsDifferential, BigDecimal fieldGoalPercentage, BigDecimal threePointPercentage,
                      BigDecimal freeThrowPercentage, BigDecimal reboundsPerGame, BigDecimal assistsPerGame,
                      BigDecimal stealsPerGame, BigDecimal blocksPerGame, BigDecimal turnoversPerGame,
                      BigDecimal offensiveRating, BigDecimal defensiveRating, BigDecimal netRating, BigDecimal pace,
                      BigDecimal effectiveFieldGoalPercentage, BigDecimal trueShootingPercentage,
                      int currentStreak, int longestWinStreak, int longestLoseStreak, String last10GamesJson) {
    }

    /**
     * @param games    matchs de saison régulière d'une saison, joués ou non (seuls les matchs joués comptent)
     * @param boxScore lignes de statistiques par match puis par équipe
     */
    static List<TeamSeason> aggregate(List<GameRow> games, Map<String, Map<Long, TeamGameRow>> boxScore) {
        Map<Long, Accumulator> teams = new LinkedHashMap<>();
        games.stream()
                .filter(game -> game.played() && !game.playoffs())
                .sorted(Comparator.comparing(GameRow::gameDate).thenComparing(GameRow::gameId))
                .forEach(game -> {
                    Map<Long, TeamGameRow> lines = boxScore.getOrDefault(game.gameId(), Map.of());
                    TeamGameRow home = lines.get(game.homeTeamId());
                    TeamGameRow away = lines.get(game.awayTeamId());
                    teams.computeIfAbsent(game.homeTeamId(), Accumulator::new)
                            .add(true, game.homePoints(), game.awayPoints(), home, away);
                    teams.computeIfAbsent(game.awayTeamId(), Accumulator::new)
                            .add(false, game.awayPoints(), game.homePoints(), away, home);
                });
        return teams.values().stream().map(Accumulator::result).toList();
    }

    private static final class Accumulator {
        private final long teamId;
        private int wins, losses, homeWins, homeLosses, awayWins, awayLosses;
        private long points, pointsAllowed;
        private final List<Boolean> results = new ArrayList<>();

        // Totaux sur les matchs dont la feuille de match est disponible (plus de 95 % des matchs)
        private int boxGames;
        private long fgm, fga, fg3m, fg3a, ftm, fta, reb, ast, stl, blk, tov, boxPoints, boxPointsAllowed;
        private double possessions, opponentPossessions;

        Accumulator(long teamId) {
            this.teamId = teamId;
        }

        void add(boolean home, int scored, int allowed, TeamGameRow own, TeamGameRow opponent) {
            boolean won = scored > allowed;
            results.add(won);
            if (won) {
                wins++;
                if (home) homeWins++; else awayWins++;
            } else {
                losses++;
                if (home) homeLosses++; else awayLosses++;
            }
            points += scored;
            pointsAllowed += allowed;

            if (own != null && opponent != null && own.fga() != null && opponent.fga() != null) {
                boxGames++;
                fgm += value(own.fgm());
                fga += value(own.fga());
                fg3m += value(own.fg3m());
                fg3a += value(own.fg3a());
                ftm += value(own.ftm());
                fta += value(own.fta());
                reb += value(own.reb());
                ast += value(own.ast());
                stl += value(own.stl());
                blk += value(own.blk());
                tov += value(own.tov());
                boxPoints += scored;
                boxPointsAllowed += allowed;
                possessions += possessions(own);
                opponentPossessions += possessions(opponent);
            }
        }

        TeamSeason result() {
            int games = wins + losses;
            BigDecimal ortg = boxGames == 0 ? null : decimal(100.0 * boxPoints / possessions, 2);
            BigDecimal drtg = boxGames == 0 ? null : decimal(100.0 * boxPointsAllowed / opponentPossessions, 2);
            return new TeamSeason(teamId, wins, losses, homeWins, homeLosses, awayWins, awayLosses,
                    decimal((double) wins / games, 3),
                    decimal((double) points / games, 2),
                    decimal((double) pointsAllowed / games, 2),
                    decimal((double) (points - pointsAllowed) / games, 2),
                    ratio(fgm, fga), ratio(fg3m, fg3a), ratio(ftm, fta),
                    perGame(reb), perGame(ast), perGame(stl), perGame(blk), perGame(tov),
                    ortg, drtg, ortg == null ? null : ortg.subtract(drtg),
                    boxGames == 0 ? null : decimal((possessions + opponentPossessions) / 2 / boxGames, 2),
                    fga == 0 ? null : decimal((fgm + 0.5 * fg3m) / fga, 3),
                    fga + fta == 0 ? null : decimal(boxPoints / (2 * (fga + 0.44 * fta)), 3),
                    currentStreak(), longestStreak(true), longestStreak(false), last10());
        }

        private BigDecimal perGame(long total) {
            return boxGames == 0 ? null : decimal((double) total / boxGames, 2);
        }

        /** Positif pour une série de victoires, négatif pour une série de défaites. */
        private int currentStreak() {
            int streak = 0;
            boolean last = results.getLast();
            for (int i = results.size() - 1; i >= 0 && results.get(i) == last; i--) {
                streak++;
            }
            return last ? streak : -streak;
        }

        private int longestStreak(boolean won) {
            int longest = 0, current = 0;
            for (boolean result : results) {
                current = result == won ? current + 1 : 0;
                longest = Math.max(longest, current);
            }
            return longest;
        }

        /** Les dix derniers résultats, du plus récent au plus ancien : ["W","L",…]. */
        private String last10() {
            List<String> last = new ArrayList<>();
            for (int i = results.size() - 1; i >= 0 && last.size() < 10; i--) {
                last.add(results.get(i) ? "\"W\"" : "\"L\"");
            }
            return "[" + String.join(",", last) + "]";
        }
    }

    static double possessions(TeamGameRow row) {
        return value(row.fga()) - value(row.oreb()) + value(row.tov()) + 0.44 * value(row.fta());
    }

    private static int value(Integer value) {
        return value == null ? 0 : value;
    }

    private static BigDecimal ratio(long made, long attempted) {
        return attempted == 0 ? null : decimal((double) made / attempted, 3);
    }

    private static BigDecimal decimal(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP);
    }
}
