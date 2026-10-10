package com.np3tech.hoopmind.nba.service.impl;

import com.np3tech.hoopmind.nba.dto.HeadToHeadDTO;
import com.np3tech.hoopmind.nba.dto.MatchDTO;
import com.np3tech.hoopmind.nba.entity.Game;
import com.np3tech.hoopmind.nba.entity.Team;
import com.np3tech.hoopmind.nba.entity.enummeration.MatchStatus;
import com.np3tech.hoopmind.shared.exception.AppException;
import com.np3tech.hoopmind.shared.exception.ErrorCode;
import com.np3tech.hoopmind.nba.repository.GameRepository;
import com.np3tech.hoopmind.nba.repository.TeamRepository;
import com.np3tech.hoopmind.nba.service.MatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchServiceImpl {

//    private final GameRepository gameRepository;
//    private final TeamRepository teamRepository;
//
//    @Transactional(readOnly = true)
//    public List<MatchDTO> getUpcomingMatches(int limit) {
//        LocalDateTime now = LocalDateTime.now();
//        return gameRepository.findUpcomingMatches(MatchStatus.SCHEDULED, now).stream()
//                .limit(limit)
//                .map(this::convertToDTO)
//                .collect(Collectors.toList());
//    }
//
//    @Transactional(readOnly = true)
//    public List<MatchDTO> getMatchesBySeason(String season) {
//        return gameRepository.findBySeason(season).stream()
//                .map(this::convertToDTO)
//                .collect(Collectors.toList());
//    }
//
//    @Transactional(readOnly = true)
//    public MatchDTO getMatchById(Long id) {
//        Match game = gameRepository.findByIdWithPrediction(id)
//                .orElseThrow(() -> new AppException("Match non trouvé", HttpStatus.NOT_FOUND, ErrorCode.MATCH_NOT_FOUND));
//        return convertToDTO(game);
//    }
//
//    @Transactional(readOnly = true)
//    public List<MatchDTO> getTeamMatches(Long teamId, String season) {
//        return gameRepository.findCompletedMatchesByTeamAndSeason(teamId, season).stream()
//                .map(this::convertToDTO)
//                .collect(Collectors.toList());
//    }
//
//    @Transactional(readOnly = true)
//    public HeadToHeadDTO getHeadToHeadAnalysis(Long team1Id, Long team2Id, int limit) {
//        Team team1 = teamRepository.findById(team1Id)
//                .orElseThrow(() -> new AppException("Équipe non trouvée", HttpStatus.NOT_FOUND, ErrorCode.TEAM_NOT_FOUND));
//        Team team2 = teamRepository.findById(team2Id)
//                .orElseThrow(() -> new AppException("Équipe non trouvée", HttpStatus.NOT_FOUND, ErrorCode.TEAM_NOT_FOUND));
//
//        List<Match> games = gameRepository.findByTeamId(team1Id).stream()
//                .filter(m -> (m.getHomeTeam().getId().equals(team2Id) || m.getAwayTeam().getId().equals(team2Id)))
//                .filter(m -> m.getStatus() == MatchStatus.COMPLETED)
//                .sorted((m1, m2) -> m2.getMatchDate().compareTo(m1.getMatchDate()))
//                .limit(limit)
//                .collect(Collectors.toList());
//
//        int team1Wins = 0;
//        double totalPointDiff = 0;
//
//        for (Match game : games) {
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
//        int team2Wins = games.size() - team1Wins;
//        double team1WinRate = games.isEmpty() ? 0 : (double) team1Wins / games.size() * 100;
//        double avgPointDiff = games.isEmpty() ? 0 : totalPointDiff / games.size();
//
//        return HeadToHeadDTO.builder()
//                .team1Id(team1Id)
//                .team1Name(team1.getName())
//                .team2Id(team2Id)
//                .team2Name(team2.getName())
//                .team1Wins(team1Wins)
//                .team2Wins(team2Wins)
//                .totalMatches(games.size())
//                .team1WinRate(team1WinRate)
//                .averagePointDifferential(avgPointDiff)
//                .recentMatches(games.stream().map(this::convertToDTO).collect(Collectors.toList()))
//                .build();
//    }
//
//    @Transactional
//    public MatchDTO createMatch(MatchDTO matchDTO) {
//        Team homeTeam = teamRepository.findById(matchDTO.getHomeTeamId())
//                .orElseThrow(() -> new AppException("Équipe domicile non trouvée", HttpStatus.NOT_FOUND, ErrorCode.TEAM_NOT_FOUND));
//        Team awayTeam = teamRepository.findById(matchDTO.getAwayTeamId())
//                .orElseThrow(() -> new AppException("Équipe extérieure non trouvée", HttpStatus.NOT_FOUND, ErrorCode.TEAM_NOT_FOUND));
//
//        Match game = Match.builder()
//                .homeTeam(homeTeam)
//                .awayTeam(awayTeam)
//                .matchDate(matchDTO.getMatchDate())
//                .status(MatchStatus.SCHEDULED)
//                .season(matchDTO.getSeason())
//                .venue(matchDTO.getVenue())
//                .build();
//
//        game = gameRepository.save(game);
//        log.info("Match créé: {} vs {} le {}", homeTeam.getName(), awayTeam.getName(), game.getMatchDate());
//
//        return convertToDTO(game);
//    }
//
//    @Transactional
//    public MatchDTO updateMatchScore(Long matchId, Integer homeScore, Integer awayScore) {
//        Game game = gameRepository.findById(matchId)
//                .orElseThrow(() -> new AppException("Match non trouvé", HttpStatus.NOT_FOUND, ErrorCode.MATCH_NOT_FOUND));
//
//        game.setHomeScore(homeScore);
//        game.setAwayScore(awayScore);
//        game.setStatus(MatchStatus.COMPLETED);
//
//        game = gameRepository.save(game);
//        log.info("Score mis à jour pour le game {}: {} - {}", matchId, homeScore, awayScore);
//
//        return convertToDTO(game);
//    }

//    private MatchDTO convertToDTO(Game game) {
//        return MatchDTO.builder()
//                .id(game.getId())
//                .homeTeamId(game.getHomeTeam().getId())
//                .homeTeamName(game.getHomeTeam().getName())
//                .awayTeamId(game.getAwayTeam().getId())
//                .awayTeamName(game.getAwayTeam().getName())
//                .matchDate(game.getScheduledDate())
////                .homeScore(game.gethom())
////                .awayScore(game.getAwayScore())
//                .status(game.getStatus())
//                .season(game.getSeason())
//                .venue(game.getVenue())
//                .build();
//    }
}
