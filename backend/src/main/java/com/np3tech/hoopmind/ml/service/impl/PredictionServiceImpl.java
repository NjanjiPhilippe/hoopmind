package com.np3tech.hoopmind.ml.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.np3tech.hoopmind.ml.dto.MatchPredictionDTO;
import com.np3tech.hoopmind.ml.dto.PredictionFactorsDTO;
import com.np3tech.hoopmind.ml.entity.*;
import com.np3tech.hoopmind.nba.entity.*;
import com.np3tech.hoopmind.nba.entity.enummeration.MatchStatus;
import com.np3tech.hoopmind.shared.exception.AppException;
import com.np3tech.hoopmind.shared.exception.ErrorCode;
import com.np3tech.hoopmind.ml.repository.*;
import com.np3tech.hoopmind.nba.repository.*;
import com.np3tech.hoopmind.ml.service.SparkAnalyticsService;
import com.np3tech.hoopmind.nba.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PredictionServiceImpl {

//    private final GameRepository gameRepository;
//    private final MatchPredictionRepository predictionRepository;
//    private final TeamRepository teamRepository;
//    private final TeamStatisticRepository teamStatisticRepository;
//    private final PlayerStatisticRepository playerStatisticRepository;
//    private final SparkAnalyticsService sparkAnalyticsService;
//    private final TeamService teamService;
//    private final ObjectMapper objectMapper;
//
//    private static final double RECENT_FORM_WEIGHT = 0.25;
//    private static final double SEASON_STATS_WEIGHT = 0.20;
//    private static final double HEAD_TO_HEAD_WEIGHT = 0.15;
//    private static final double HOME_ADVANTAGE_WEIGHT = 0.10;
//    private static final double PLAYER_STATS_WEIGHT = 0.20;
//    private static final double ADVANCED_STATS_WEIGHT = 0.10;
//    private static final int RECENT_MATCHES_COUNT = 10;
//    private static final int HEAD_TO_HEAD_COUNT = 5;
//
//    @Transactional
//    public MatchPredictionDTO predictMatch(Long matchId) {
//        Match game = gameRepository.findById(matchId)
//                .orElseThrow(() -> new AppException("Match non trouvé", HttpStatus.NOT_FOUND, ErrorCode.MATCH_NOT_FOUND));
//
//        if (game.getStatus() == MatchStatus.COMPLETED) {
//            throw new AppException("Le game est déjà terminé", HttpStatus.BAD_REQUEST, ErrorCode.MATCH_ALREADY_PLAYED);
//        }
//
//        // Vérifier si une prédiction existe déjà
//        Optional<MatchPrediction> existingPrediction = predictionRepository.findByMatchId(matchId);
//        if (existingPrediction.isPresent()) {
//            return convertToDTO(existingPrediction.get());
//        }
//
//        Team homeTeam = game.getHomeTeam();
//        Team awayTeam = game.getAwayTeam();
//        String season = game.getSeason();
//
//        log.info("Génération de la prédiction pour {} vs {}", homeTeam.getName(), awayTeam.getName());
//
//        // 1. Analyse de la forme récente des équipes
//        PredictionFactorsDTO factors = new PredictionFactorsDTO();
//        double homeFormScore = analyzeTeamRecentForm(homeTeam.getId(), factors);
//        double awayFormScore = analyzeTeamRecentForm(awayTeam.getId(), factors);
//
//        // 2. Analyse des statistiques de la saison
//        double homeSeasonScore = analyzeSeasonStatistics(homeTeam.getId(), season, factors);
//        double awaySeasonScore = analyzeSeasonStatistics(awayTeam.getId(), season, factors);
//
//        // 3. Analyse des confrontations directes
//        double headToHeadScore = analyzeHeadToHead(homeTeam.getId(), awayTeam.getId(), factors);
//
//        // 4. Avantage du terrain
//        double homeAdvantageScore = calculateHomeAdvantage(homeTeam.getId(), season, factors);
//
//        // 5. Analyse des statistiques des joueurs (VORPM, PER, etc.)
//        double homePlayerScore = analyzePlayerStatistics(homeTeam.getId(), season, factors);
//        double awayPlayerScore = analyzePlayerStatistics(awayTeam.getId(), season, factors);
//
//        // 6. Statistiques avancées (efficacité offensive/défensive)
//        double homeAdvancedScore = analyzeAdvancedStatistics(homeTeam.getId(), season, factors);
//        double awayAdvancedScore = analyzeAdvancedStatistics(awayTeam.getId(), season, factors);
//
//        // Calcul du score pondéré total pour chaque équipe
//        double homeScore = (homeFormScore * RECENT_FORM_WEIGHT) +
//                (homeSeasonScore * SEASON_STATS_WEIGHT) +
//                (headToHeadScore * HEAD_TO_HEAD_WEIGHT) +
//                (homeAdvantageScore * HOME_ADVANTAGE_WEIGHT) +
//                (homePlayerScore * PLAYER_STATS_WEIGHT) +
//                (homeAdvancedScore * ADVANCED_STATS_WEIGHT);
//
//        double awayScore = (awayFormScore * RECENT_FORM_WEIGHT) +
//                (awaySeasonScore * SEASON_STATS_WEIGHT) +
//                (-headToHeadScore * HEAD_TO_HEAD_WEIGHT) +  // Inverse pour l'équipe extérieure
//                (awayPlayerScore * PLAYER_STATS_WEIGHT) +
//                (awayAdvancedScore * ADVANCED_STATS_WEIGHT);
//
//        // Normaliser les scores en probabilités
//        double totalScore = homeScore + awayScore;
//        double homeWinProbability = (homeScore / totalScore) * 100;
//        double awayWinProbability = (awayScore / totalScore) * 100;
//
//        // Prédiction des scores basée sur les moyennes et les probabilités
//        TeamStats homeStats = teamStatisticRepository.findByTeamIdAndSeason(homeTeam.getId(), season)
//                .orElse(null);
//        TeamStats awayStats = teamStatisticRepository.findByTeamIdAndSeason(awayTeam.getId(), season)
//                .orElse(null);
//
//        double predictedHomeScore = calculatePredictedScore(homeStats, awayStats, homeWinProbability, true);
//        double predictedAwayScore = calculatePredictedScore(awayStats, homeStats, awayWinProbability, false);
//
//        // Calcul du score de confiance
//        double confidenceScore = calculateConfidenceScore(factors, Math.abs(homeWinProbability - awayWinProbability));
//
//        // Utilisation de Spark pour affiner la prédiction avec ML
//        try {
//            Map<String, Object> sparkPrediction = sparkAnalyticsService.refinePrediction(
//                    game, homeScore, awayScore, factors);
//
//            if (sparkPrediction != null) {
//                homeWinProbability = (Double) sparkPrediction.get("homeWinProbability");
//                awayWinProbability = (Double) sparkPrediction.get("awayWinProbability");
//                predictedHomeScore = (Double) sparkPrediction.get("predictedHomeScore");
//                predictedAwayScore = (Double) sparkPrediction.get("predictedAwayScore");
//                confidenceScore = (Double) sparkPrediction.get("confidenceScore");
//            }
//        } catch (Exception e) {
//            log.warn("Échec de l'affinement Spark, utilisation de la prédiction de base", e);
//        }
//
//        // Sauvegarder la prédiction
//        String factorsJson;
//        try {
//            factorsJson = objectMapper.writeValueAsString(factors);
//        } catch (JsonProcessingException e) {
//            factorsJson = "{}";
//            log.error("Erreur lors de la sérialisation des facteurs", e);
//        }
//
//        MatchPrediction prediction = MatchPrediction.builder()
//                .game(game)
//                .predictedHomeScore(predictedHomeScore)
//                .predictedAwayScore(predictedAwayScore)
//                .homeWinProbability(homeWinProbability)
//                .awayWinProbability(awayWinProbability)
//                .confidenceScore(confidenceScore)
//                .modelVersion("v1.0")
//                .factors(factorsJson)
//                .build();
//
//        prediction = predictionRepository.save(prediction);
//        log.info("Prédiction sauvegardée: {} - {}% vs {} - {}%",
//                homeTeam.getName(), homeWinProbability, awayTeam.getName(), awayWinProbability);
//
//        return convertToDTO(prediction);
//    }
//
//    private double analyzeTeamRecentForm(Long teamId, PredictionFactorsDTO factors) {
//        try {
//            var teamForm = teamService.getTeamForm(teamId, RECENT_MATCHES_COUNT);
//            factors.addFactor("forme_equipe_" + teamId,
//                    String.format("%.1f%% victoires - Diff: %.1f pts",
//                            teamForm.getWinRate(), teamForm.getPointDifferential()));
//
//            // Score basé sur le taux de victoire et la différence de points
//            double formScore = (teamForm.getWinRate() / 100) * 0.6 +
//                    (Math.min(teamForm.getPointDifferential() / 20, 1.0)) * 0.4;
//
//            // Bonus si en série de victoires
//            if (teamForm.isOnWinStreak()) {
//                formScore += 0.1;
//                factors.addFactor("serie_victoires_" + teamId, "Série de 3+ victoires");
//            }
//
//            return formScore;
//        } catch (Exception e) {
//            log.warn("Impossible d'analyser la forme récente pour l'équipe {}", teamId);
//            return 0.5; // Score neutre
//        }
//    }
//
//    private double analyzeSeasonStatistics(Long teamId, String season, PredictionFactorsDTO factors) {
//        Optional<TeamStats> statsOpt = teamStatisticRepository.findByTeamIdAndSeason(teamId, season);
//        if (statsOpt.isEmpty()) {
//            return 0.5;
//        }
//
//        TeamStats stats = statsOpt.get();
//        factors.addFactor("stats_saison_" + teamId,
//                String.format("%d-%d (%.1f%%) - %.1f pts/game",
//                        stats.getWins(), stats.getLosses(),
//                        stats.getWinPercentage() * 100, stats.getPointsPerGame()));
//
//        // Score basé sur le pourcentage de victoires et la différence de points
//        double winScore = stats.getWinPercentage();
//        double pointDiff = stats.getPointsPerGame() - stats.getPointsAllowedPerGame();
//        double pointDiffScore = Math.min((pointDiff + 10) / 20, 1.0);
//
//        return (winScore * 0.7) + (pointDiffScore * 0.3);
//    }
//
//    private double analyzeHeadToHead(Long team1Id, Long team2Id, PredictionFactorsDTO factors) {
//        List<Match> headToHead = teamService.getHeadToHeadMatches(team1Id, team2Id, HEAD_TO_HEAD_COUNT);
//
//        if (headToHead.isEmpty()) {
//            factors.addFactor("confrontations_directes", "Aucune confrontation récente");
//            return 0.0; // Neutre si pas de données
//        }
//
//        int team1Wins = 0;
//        double totalPointDiff = 0;
//
//        for (Match game : headToHead) {
//            boolean team1IsHome = game.getHomeTeam().getId().equals(team1Id);
//            int team1Score = team1IsHome ? game.getHomeScore() : game.getAwayScore();
//            int team2Score = team1IsHome ? game.getAwayScore() : game.getHomeScore();
//
//            if (team1Score > team2Score) {
//                team1Wins++;
//            }
//            totalPointDiff += (team1Score - team2Score);
//        }
//
//        double winRate = (double) team1Wins / headToHead.size();
//        double avgPointDiff = totalPointDiff / headToHead.size();
//
//        factors.addFactor("confrontations_directes",
//                String.format("%d victoires sur %d matchs - Diff moyenne: %.1f pts",
//                        team1Wins, headToHead.size(), avgPointDiff));
//
//        return (winRate * 0.7) + (Math.min((avgPointDiff + 10) / 20, 1.0) * 0.3);
//    }
//
//    private double calculateHomeAdvantage(Long teamId, String season, PredictionFactorsDTO factors) {
//        Optional<TeamStats> statsOpt = teamStatisticRepository.findByTeamIdAndSeason(teamId, season);
//        if (statsOpt.isEmpty()) {
//            return 0.55; // Avantage léger par défaut
//        }
//
//        TeamStats stats = statsOpt.get();
//        String homeRecord = stats.getHomeRecord();
//
//        if (homeRecord != null && homeRecord.contains("-")) {
//            String[] parts = homeRecord.split("-");
//            int homeWins = Integer.parseInt(parts[0]);
//            int homeLosses = Integer.parseInt(parts[1]);
//            double homeWinRate = (double) homeWins / (homeWins + homeLosses);
//
//            factors.addFactor("avantage_terrain",
//                    String.format("Bilan à domicile: %s (%.1f%%)", homeRecord, homeWinRate * 100));
//
//            return 0.5 + (homeWinRate * 0.15); // Max 0.65
//        }
//
//        return 0.55;
//    }
//
//    private double analyzePlayerStatistics(Long teamId, String season, PredictionFactorsDTO factors) {
//        List<PlayerStats> playerStats = playerStatisticRepository.findByTeamAndSeason(teamId, season);
//
//        if (playerStats.isEmpty()) {
//            return 0.5;
//        }
//
//        // Calcul des statistiques agrégées de l'équipe basées sur les joueurs
//        double totalPER = 0;
//        double totalVORP = 0; // Value Over Replacement Player (simulé)
//        double totalEfficiency = 0;
//        int playerCount = Math.min(playerStats.size(), 10); // Top 10 joueurs
//
//        List<PlayerStats> topPlayers = playerStats.stream()
//                .sorted((p1, p2) -> Double.compare(p2.getPointsPerGame(), p1.getPointsPerGame()))
//                .limit(playerCount)
//                .collect(Collectors.toList());
//
//        for (PlayerStats stat : topPlayers) {
//            double per = calculatePlayerPER(stat);
//            double vorp = calculateVORP(stat);
//            double efficiency = calculatePlayerEfficiency(stat);
//
//            totalPER += per;
//            totalVORP += vorp;
//            totalEfficiency += efficiency;
//        }
//
//        double avgPER = totalPER / playerCount;
//        double avgVORP = totalVORP / playerCount;
//        double avgEfficiency = totalEfficiency / playerCount;
//
//        factors.addFactor("stats_joueurs_" + teamId,
//                String.format("PER moyen: %.1f - VORP: %.1f - Eff: %.1f",
//                        avgPER, avgVORP, avgEfficiency));
//
//        // Normalisation du score (PER moyen NBA ~15, VORP ~2, Efficacité ~15)
//        double perScore = Math.min(avgPER / 20, 1.0);
//        double vorpScore = Math.min(avgVORP / 4, 1.0);
//        double efficiencyScore = Math.min(avgEfficiency / 20, 1.0);
//
//        return (perScore * 0.4) + (vorpScore * 0.3) + (efficiencyScore * 0.3);
//    }
//
//    private double calculatePlayerPER(PlayerStats stat) {
//        // PER simplifié
//        return stat.getPointsPerGame() + stat.getReboundsPerGame() +
//                stat.getAssistsPerGame() + stat.getStealsPerGame() +
//                stat.getBlocksPerGame() - stat.getTurnoversPerGame() +
//                (stat.getFieldGoalPercentage() / 10);
//    }
//
//    private double calculateVORP(PlayerStats stat) {
//        // VORP simulé basé sur la contribution globale
//        double boxScore = stat.getPointsPerGame() + stat.getReboundsPerGame() +
//                stat.getAssistsPerGame() + stat.getStealsPerGame() + stat.getBlocksPerGame();
//        return (boxScore - 10) / 5; // Normalisation
//    }
//
//    private double calculatePlayerEfficiency(PlayerStats stat) {
//        return stat.getPointsPerGame() + stat.getReboundsPerGame() +
//                stat.getAssistsPerGame() + stat.getStealsPerGame() +
//                stat.getBlocksPerGame() - stat.getTurnoversPerGame();
//    }
//
//    private double analyzeAdvancedStatistics(Long teamId, String season, PredictionFactorsDTO factors) {
//        Optional<TeamStats> statsOpt = teamStatisticRepository.findByTeamIdAndSeason(teamId, season);
//        if (statsOpt.isEmpty()) {
//            return 0.5;
//        }
//
//        TeamStats stats = statsOpt.get();
//
//        // Efficacité offensive (points marqués)
//        double offensiveEfficiency = Math.min(stats.getPointsPerGame() / 120, 1.0);
//
//        // Efficacité défensive (points encaissés - inverse)
//        double defensiveEfficiency = Math.max(0, 1.0 - (stats.getPointsAllowedPerGame() / 120));
//
//        // Efficacité au tir
//        double shootingEfficiency = (stats.getFieldGoalPercentage() +
//                stats.getThreePointPercentage() +
//                stats.getFreeThrowPercentage()) / 300;
//
//        factors.addFactor("stats_avancees_" + teamId,
//                String.format("Off: %.1f - Def: %.1f - Tir: %.1f%%",
//                        offensiveEfficiency * 100, defensiveEfficiency * 100,
//                        shootingEfficiency * 100));
//
//        return (offensiveEfficiency * 0.4) + (defensiveEfficiency * 0.4) + (shootingEfficiency * 0.2);
//    }
//
//    private double calculatePredictedScore(TeamStats teamStats, TeamStats opponentStats,
//                                           double winProbability, boolean isHome) {
//        if (teamStats == null) {
//            return 105.0; // Score moyen NBA
//        }
//
//        double baseScore = teamStats.getPointsPerGame();
//        double opponentDefense = opponentStats != null ? opponentStats.getPointsAllowedPerGame() : 105.0;
//
//        // Ajustement basé sur la probabilité de victoire
//        double probabilityAdjustment = (winProbability - 50) / 10;
//
//        // Ajustement pour l'avantage du terrain
//        double homeAdjustment = isHome ? 2.0 : 0.0;
//
//        // Ajustement basé sur la défense adverse
//        double defenseAdjustment = (105.0 - opponentDefense) * 0.3;
//
//        return baseScore + probabilityAdjustment + homeAdjustment + defenseAdjustment;
//    }
//
//    private double calculateConfidenceScore(PredictionFactorsDTO factors, double probabilityGap) {
//        // Base: écart de probabilité (plus l'écart est grand, plus on est confiant)
//        double baseConfidence = Math.min(probabilityGap / 40, 1.0) * 50;
//
//        // Bonus basé sur la quantité de données disponibles
//        int factorCount = factors.getFactors().size();
//        double dataBonus = Math.min(factorCount / 10.0, 1.0) * 30;
//
//        // Bonus pour les séries de victoires identifiées
//        double streakBonus = factors.getFactors().values().stream()
//                .anyMatch(v -> v.contains("Série")) ? 10 : 0;
//
//        return Math.min(baseConfidence + dataBonus + streakBonus, 100.0);
//    }
//
//    @Transactional(readOnly = true)
//    public List<MatchPredictionDTO> getUpcomingPredictions(int limit) {
//        LocalDateTime now = LocalDateTime.now();
//        List<Match> upcomingGames = gameRepository.findUpcomingMatches(MatchStatus.SCHEDULED, now)
//                .stream()
//                .limit(limit)
//                .collect(Collectors.toList());
//
//        return upcomingGames.stream()
//                .map(match -> {
//                    Optional<MatchPrediction> prediction = predictionRepository.findByMatchId(match.getId());
//                    return prediction.map(this::convertToDTO).orElse(null);
//                })
//                .filter(Objects::nonNull)
//                .collect(Collectors.toList());
//    }
//
//    @Transactional(readOnly = true)
//    public MatchPredictionDTO getPredictionById(Long predictionId) {
//        MatchPrediction prediction = predictionRepository.findById(predictionId)
//                .orElseThrow(() -> new AppException("Prédiction non trouvée",
//                        HttpStatus.NOT_FOUND, ErrorCode.PREDICTION_NOT_FOUND));
//        return convertToDTO(prediction);
//    }
//
//    private MatchPredictionDTO convertToDTO(MatchPrediction prediction) {
//        Match game = prediction.getGame();
//
//        return MatchPredictionDTO.builder()
//                .id(prediction.getId())
//                .matchId(game.getId())
//                .homeTeamName(game.getHomeTeam().getName())
//                .awayTeamName(game.getAwayTeam().getName())
//                .matchDate(game.getMatchDate())
//                .predictedHomeScore(prediction.getPredictedHomeScore())
//                .predictedAwayScore(prediction.getPredictedAwayScore())
//                .homeWinProbability(prediction.getHomeWinProbability())
//                .awayWinProbability(prediction.getAwayWinProbability())
//                .confidenceScore(prediction.getConfidenceScore())
//                .predictionDate(prediction.getPredictionDate())
//                .modelVersion(prediction.getModelVersion())
//                .factors(prediction.getFactors())
//                .build();
//    }
}
