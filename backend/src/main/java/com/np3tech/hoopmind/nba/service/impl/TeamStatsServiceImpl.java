package com.np3tech.hoopmind.nba.service.impl;

import com.np3tech.hoopmind.nba.entity.Season;
import com.np3tech.hoopmind.nba.entity.Team;
import com.np3tech.hoopmind.nba.entity.TeamStats;
import com.np3tech.hoopmind.nba.entity.enummeration.Conference;
import com.np3tech.hoopmind.shared.exception.AppException;
import com.np3tech.hoopmind.nba.repository.TeamStatsRepository;
import com.np3tech.hoopmind.nba.service.SeasonService;
import com.np3tech.hoopmind.nba.service.TeamService;
import com.np3tech.hoopmind.nba.service.TeamStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TeamStatsServiceImpl implements TeamStatsService {

    private final TeamStatsRepository teamStatsRepository;
    private final TeamService teamService;
    private final SeasonService seasonService;

    public TeamStats createOrUpdateTeamStats(TeamStats teamStats) {
        log.info("Creating/Updating team stats for team id: {} and season id: {}",
                teamStats.getTeam().getId(), teamStats.getSeason().getId());

        validateTeamStats(teamStats);

        teamStats.setLastUpdated(LocalDateTime.now());

        return teamStatsRepository.findByTeamAndSeason(teamStats.getTeam(), teamStats.getSeason())
                .map(existing -> {
                    updateExistingStats(existing, teamStats);
                    return teamStatsRepository.save(existing);
                })
                .orElseGet(() -> teamStatsRepository.save(teamStats));
    }

    @Transactional(readOnly = true)
    public TeamStats getTeamStatsById(Long id) {
        return teamStatsRepository.findById(id)
                .orElseThrow(() -> new AppException(
                        "Team stats not found with id: " + id,
                        HttpStatus.NOT_FOUND,
                        "TEAM_STATS_NOT_FOUND"
                ));
    }

    @Transactional(readOnly = true)
    public TeamStats getTeamStatsByTeamAndSeason(Long teamId, Long seasonId) {
        Team team = teamService.getTeamById(teamId);
        Season season = seasonService.getSeasonById(seasonId);

        return teamStatsRepository.findByTeamAndSeason(team, season)
                .orElseThrow(() -> new AppException(
                        "Team stats not found for team id: " + teamId + " and season id: " + seasonId,
                        HttpStatus.NOT_FOUND,
                        "TEAM_STATS_NOT_FOUND"
                ));
    }

    @Transactional(readOnly = true)
    public List<TeamStats> getTeamStatsBySeason(Long seasonId) {
        Season season = seasonService.getSeasonById(seasonId);
        return teamStatsRepository.findBySeason(season);
    }

    @Transactional(readOnly = true)
    public List<TeamStats> getStandingsBySeason(Long seasonId) {
        Season season = seasonService.getSeasonById(seasonId);
        return teamStatsRepository.findStandingsBySeason(season);
    }

    @Transactional(readOnly = true)
    public List<TeamStats> getConferenceStandings(Long seasonId, Conference conference) {
        Season season = seasonService.getSeasonById(seasonId);
        return teamStatsRepository.findConferenceStandings(season, conference);
    }

    public void deleteTeamStats(Long id) {
        log.info("Deleting team stats with id: {}", id);

        if (!teamStatsRepository.existsById(id)) {
            throw new AppException(
                    "Team stats not found with id: " + id,
                    HttpStatus.NOT_FOUND,
                    "TEAM_STATS_NOT_FOUND"
            );
        }

        teamStatsRepository.deleteById(id);
    }

    private void validateTeamStats(TeamStats teamStats) {
        if (teamStats.getTeam() == null) {
            throw new AppException(
                    "Team is required for team stats",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_TEAM_STATS_DATA"
            );
        }

        if (teamStats.getSeason() == null) {
            throw new AppException(
                    "Season is required for team stats",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_TEAM_STATS_DATA"
            );
        }
    }

    private void updateExistingStats(TeamStats existing, TeamStats updated) {
        if (updated.getWins() != null) existing.setWins(updated.getWins());
        if (updated.getLosses() != null) existing.setLosses(updated.getLosses());
        if (updated.getWinPercentage() != null) existing.setWinPercentage(updated.getWinPercentage());
        if (updated.getPointsPerGame() != null) existing.setPointsPerGame(updated.getPointsPerGame());
        if (updated.getPointsAllowedPerGame() != null) existing.setPointsAllowedPerGame(updated.getPointsAllowedPerGame());
        if (updated.getOffensiveRating() != null) existing.setOffensiveRating(updated.getOffensiveRating());
        if (updated.getDefensiveRating() != null) existing.setDefensiveRating(updated.getDefensiveRating());
        existing.setLastUpdated(LocalDateTime.now());
    }
}
