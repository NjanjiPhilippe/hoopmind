package com.np3tech.hoopmind.nba.service.impl;

import com.np3tech.hoopmind.nba.dto.PlayerStatisticDTO;
import com.np3tech.hoopmind.nba.dto.StatisticsOverviewDTO;
import com.np3tech.hoopmind.nba.dto.TeamFormDTO;
import com.np3tech.hoopmind.nba.dto.TeamStatisticDTO;
import com.np3tech.hoopmind.nba.entity.PlayerStats;
import com.np3tech.hoopmind.nba.entity.TeamStats;
import com.np3tech.hoopmind.nba.repository.PlayerStatisticRepository;
import com.np3tech.hoopmind.nba.repository.TeamStatisticRepository;
import com.np3tech.hoopmind.nba.service.StatisticsService;
import com.np3tech.hoopmind.nba.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsServiceImpl implements StatisticsService {
    private final TeamStatisticRepository teamStatisticRepository;
    private final PlayerStatisticRepository playerStatisticRepository;
    private final TeamService teamService;

    @Transactional(readOnly = true)
    public StatisticsOverviewDTO getTeamStatisticsOverview(Long teamId, Integer season) {
        log.info("Construction vue d'ensemble statistiques pour l'équipe {} - saison {}", teamId, season);

        TeamStats teamStats = teamStatisticRepository.findByTeamIdAndSeason_Year(teamId, season)
                .orElse(null);

        TeamStatisticDTO teamStatsDTO = teamStats != null ? convertTeamStatToDTO(teamStats) : null;

        List<PlayerStatisticDTO> topScorers = playerStatisticRepository
                .findByTeamAndSeason(teamId, season)
                .stream()
                .sorted((p1, p2) -> p2.getPointsPerGame().compareTo(p1.getPointsPerGame()))
                .limit(5)
                .map(this::convertPlayerStatToDTO)
                .toList();

        List<PlayerStatisticDTO> topRebounders = playerStatisticRepository
                .findByTeamAndSeason(teamId, season)
                .stream()
                .sorted((p1, p2) -> p2.getTotalReboundsPerGame().compareTo(p1.getTotalReboundsPerGame()))
                .limit(5)
                .map(this::convertPlayerStatToDTO)
                .collect(Collectors.toList());

        List<PlayerStatisticDTO> topAssistants = playerStatisticRepository
                .findByTeamAndSeason(teamId, season)
                .stream()
                .sorted((p1, p2) -> p2.getAssistsPerGame().compareTo(p1.getAssistsPerGame()))
                .limit(5)
                .map(this::convertPlayerStatToDTO)
                .collect(Collectors.toList());

        TeamFormDTO recentForm = null;
        try {
            recentForm = teamService.getTeamForm(teamId, 10);
        } catch (Exception e) {
            log.warn("Impossible de récupérer la forme récente de l'équipe {}", teamId);
        }

        return StatisticsOverviewDTO.builder()
                .season(season)
                .teamStats(teamStatsDTO)
                .topScorers(topScorers)
                .topRebounders(topRebounders)
                .topAssistants(topAssistants)
                .recentForm(recentForm)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TeamStatisticDTO> getTopTeams(Integer season, int limit) {
        log.info("Récupération du top {} équipes pour la saison {}", limit, season);

        return teamStatisticRepository.findTopTeamsBySeason(season)
                .stream()
                .limit(limit)
                .map(this::convertTeamStatToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PlayerStatisticDTO> getTopScorers(Integer season, int limit) {
        log.info("Récupération du top {} scoreurs pour la saison {}", limit, season);

        return playerStatisticRepository.findTopScorers(season)
                .stream()
                .limit(limit)
                .map(this::convertPlayerStatToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PlayerStatisticDTO> getTopAssistants(Integer season, int limit) {
        log.info("Récupération du top {} passeurs pour la saison {}", limit, season);

        return playerStatisticRepository.findTopAssistants(season)
                .stream()
                .limit(limit)
                .map(this::convertPlayerStatToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TeamStatisticDTO> getConferenceStandings(String conference, Integer season) {
        log.info("Récupération du classement de la conférence {} - saison {}", conference, season);

        return teamStatisticRepository.findByConferenceAndSeason(conference, season)
                .stream()
                .map(this::convertTeamStatToDTO)
                .collect(Collectors.toList());
    }

    private TeamStatisticDTO convertTeamStatToDTO(TeamStats stat) {
        return TeamStatisticDTO.builder()
                .id(stat.getId())
                .teamId(stat.getTeam().getId())
                .teamName(stat.getTeam().getName())
                .season(stat.getSeason().getName())
                .wins(stat.getWins())
                .losses(stat.getLosses())
                .winPercentage(stat.getWinPercentage())
                .pointsPerGame(stat.getPointsPerGame())
                .pointsAllowedPerGame(stat.getPointsAllowedPerGame())
                .fieldGoalPercentage(stat.getFieldGoalPercentage())
                .threePointPercentage(stat.getThreePointPercentage())
                .freeThrowPercentage(stat.getFreeThrowPercentage())
                .reboundsPerGame(stat.getReboundsPerGame())
                .assistsPerGame(stat.getAssistsPerGame())
//                .homeRecord(stat.get())
//                .awayRecord(stat.getAwayRecord())
                .build();
    }

    private PlayerStatisticDTO convertPlayerStatToDTO(PlayerStats stat) {
        return PlayerStatisticDTO.builder()
                .id(stat.getId())
                .playerId(stat.getPlayer().getId())
                .playerName(stat.getPlayer().getFirstName() + " " + stat.getPlayer().getLastName())
                .season(stat.getSeason().getName())
                .gamesPlayed(stat.getGamesPlayed())
                .minutesPerGame(stat.getMinutesPerGame())
                .pointsPerGame(stat.getPointsPerGame())
                .reboundsPerGame(stat.getTotalReboundsPerGame())
                .assistsPerGame(stat.getAssistsPerGame())
                .stealsPerGame(stat.getStealsPerGame())
                .blocksPerGame(stat.getBlocksPerGame())
                .fieldGoalPercentage(stat.getFieldGoalPercentage())
                .threePointPercentage(stat.getThreePointPercentage())
                .freeThrowPercentage(stat.getFreeThrowPercentage())
                .turnoversPerGame(stat.getTurnoversPerGame())
                .statDate(stat.getLastUpdated().toLocalDate())
                .build();
    }
}
