package com.np3tech.hoopmind.nba.service;

import com.np3tech.hoopmind.ml.entity.*;
import com.np3tech.hoopmind.nba.entity.*;
import com.np3tech.hoopmind.nba.entity.enummeration.InjuryStatus;
import com.np3tech.hoopmind.ml.repository.*;
import com.np3tech.hoopmind.nba.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Service for generating reports and analytics
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportingService {

    private final TeamStatsRepository teamStatsRepository;
    private final PlayerStatsRepository playerStatsRepository;
    private final GameRepository gameRepository;
    private final GameResultRepository gameResultRepository;
    private final MatchPredictionRepository matchPredictionRepository;
    private final InjuryReportRepository injuryReportRepository;

    /**
     * Generate season summary report
     */
    public Map<String, Object> generateSeasonSummary(Season season) {
        log.info("Generating season summary for season: {}", season.getName());

        Map<String, Object> summary = new HashMap<>();

        // Get all games for the season
        List<Game> games = gameRepository.findBySeason(season);
        long completedGames = games.stream()
                .filter(g -> g.getStatus().name().equals("FINISHED"))
                .count();

        // Team statistics
        List<TeamStats> teamStats = teamStatsRepository.findStandingsBySeason(season);

        summary.put("seasonName", season.getName());
        summary.put("seasonYear", season.getYear());
        summary.put("totalGames", games.size());
        summary.put("completedGames", completedGames);
        summary.put("standings", teamStats);

        // Top teams
        if (!teamStats.isEmpty()) {
            summary.put("topTeam", teamStats.get(0));
            summary.put("averageWinPercentage", calculateAverageWinPercentage(teamStats));
        }

        return summary;
    }

    /**
     * Generate player leaderboard
     */
    public Map<String, List<PlayerStats>> generatePlayerLeaderboard(Season season, int limit) {
        log.info("Generating player leaderboard for season: {}", season.getName());

        Map<String, List<PlayerStats>> leaderboard = new HashMap<>();

        List<PlayerStats> allStats = playerStatsRepository.findBySeason(season);

        // Top scorers
        leaderboard.put("topScorers", allStats.stream()
                .filter(s -> s.getPointsPerGame() != null)
                .sorted(Comparator.comparing(PlayerStats::getPointsPerGame).reversed())
                .limit(limit)
                .collect(Collectors.toList()));

        // Top rebounders
        leaderboard.put("topRebounders", allStats.stream()
                .filter(s -> s.getTotalReboundsPerGame() != null)
                .sorted(Comparator.comparing(PlayerStats::getTotalReboundsPerGame).reversed())
                .limit(limit)
                .collect(Collectors.toList()));

        // Top assists
        leaderboard.put("topAssists", allStats.stream()
                .filter(s -> s.getAssistsPerGame() != null)
                .sorted(Comparator.comparing(PlayerStats::getAssistsPerGame).reversed())
                .limit(limit)
                .collect(Collectors.toList()));

        // Best shooters (FG%)
        leaderboard.put("bestShooters", allStats.stream()
                .filter(s -> s.getFieldGoalPercentage() != null && s.getGamesPlayed() >= 10)
                .sorted(Comparator.comparing(PlayerStats::getFieldGoalPercentage).reversed())
                .limit(limit)
                .collect(Collectors.toList()));

        return leaderboard;
    }

    /**
     * Generate injury report summary
     */
    public Map<String, Object> generateInjuryReport(Season season) {
        log.info("Generating injury report");

        Map<String, Object> report = new HashMap<>();

        List<InjuryReport> activeInjuries = injuryReportRepository.findActiveByStatuses(
                Arrays.asList(
                        InjuryStatus.OUT_FOR_SEASON,
                        InjuryStatus.MINOR_INJURY,
                        InjuryStatus.MAJOR_INJURY
                )
        );

        // Group by status
        Map<String, Long> byStatus = activeInjuries.stream()
                .collect(Collectors.groupingBy(
                        ir -> ir.getStatus().name(),
                        Collectors.counting()
                ));

        // Group by team
        Map<String, Long> byTeam = activeInjuries.stream()
                .collect(Collectors.groupingBy(
                        ir -> ir.getTeam().getName(),
                        Collectors.counting()
                ));

        report.put("totalActiveInjuries", activeInjuries.size());
        report.put("byStatus", byStatus);
        report.put("byTeam", byTeam);
        report.put("detailedReport", activeInjuries);

        return report;
    }

    /**
     * Generate prediction accuracy report
     */
    public Map<String, Object> generatePredictionAccuracyReport(String model) {
        log.info("Generating prediction accuracy report for model: {}", model);

        Map<String, Object> report = new HashMap<>();

        List<MatchPrediction> evaluatedPredictions = model != null
                ? matchPredictionRepository.findCorrectPredictionsByModel(model)
                : matchPredictionRepository.findEvaluatedPredictions();

        long total = evaluatedPredictions.size();
        long correct = evaluatedPredictions.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsAccurate()))
                .count();

        double accuracy = total > 0 ? (double) correct / total : 0;

        // Calculate average prediction error
        double avgError = evaluatedPredictions.stream()
                .filter(p -> p.getPredictionError() != null)
                .mapToDouble(p -> p.getPredictionError().doubleValue())
                .average()
                .orElse(0.0);

        report.put("model", model != null ? model : "All Models");
        report.put("totalPredictions", total);
        report.put("correctPredictions", correct);
        report.put("accuracy", BigDecimal.valueOf(accuracy).setScale(4, BigDecimal.ROUND_HALF_UP));
        report.put("averagePredictionError", BigDecimal.valueOf(avgError).setScale(2, BigDecimal.ROUND_HALF_UP));

        return report;
    }

    /**
     * Generate head-to-head statistics
     */
    public Map<String, Object> generateHeadToHeadStats(Team team1, Team team2, Season season) {
        log.info("Generating head-to-head stats: {} vs {}", team1.getName(), team2.getName());

        Map<String, Object> stats = new HashMap<>();

        List<Game> games = gameRepository.findBySeasonAndTeam(season, team1).stream()
                .filter(g -> g.getHomeTeam().getId().equals(team2.getId()) ||
                        g.getAwayTeam().getId().equals(team2.getId()))
                .collect(Collectors.toList());

        AtomicInteger team1Wins = new AtomicInteger();
        AtomicInteger team2Wins = new AtomicInteger();

        for (Game game : games) {
            gameResultRepository.findByGame(game).ifPresent(result -> {
                if (result.getWinner().getId().equals(team1.getId())) {
                    team1Wins.getAndIncrement();
                } else {
                    team2Wins.getAndIncrement();
                }
            });
        }

        stats.put("team1", team1.getName());
        stats.put("team2", team2.getName());
        stats.put("totalGames", games.size());
        stats.put("team1Wins", team1Wins);
        stats.put("team2Wins", team2Wins);
        stats.put("games", games);

        return stats;
    }

    // Helper methods
    private BigDecimal calculateAverageWinPercentage(List<TeamStats> teamStats) {
        if (teamStats.isEmpty()) return BigDecimal.ZERO;

        double avg = teamStats.stream()
                .filter(ts -> ts.getWinPercentage() != null)
                .mapToDouble(ts -> ts.getWinPercentage().doubleValue())
                .average()
                .orElse(0.0);

        return BigDecimal.valueOf(avg).setScale(3, BigDecimal.ROUND_HALF_UP);
    }
}
