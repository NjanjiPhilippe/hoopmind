package com.np3tech.hoopmind.nba.service;

import com.np3tech.hoopmind.nba.dto.request.InjuryReportCreateRequest;
import com.np3tech.hoopmind.nba.dto.request.InjuryReportUpdateRequest;
import com.np3tech.hoopmind.nba.entity.InjuryReport;
import com.np3tech.hoopmind.nba.entity.Player;
import com.np3tech.hoopmind.nba.entity.Team;
import com.np3tech.hoopmind.nba.entity.enummeration.InjuryStatus;
import com.np3tech.hoopmind.shared.exception.AppException;
import com.np3tech.hoopmind.nba.repository.InjuryReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InjuryReportService {

    private final InjuryReportRepository injuryReportRepository;
    private final PlayerService playerService;
    private final TeamService teamService;
    private final Clock clock;

    public InjuryReport createInjuryReport(InjuryReportCreateRequest request) {
        Player player = playerService.getPlayerById(request.getPlayerId());
        Team team = teamService.getTeamById(request.getTeamId());

        InjuryReport injuryReport = InjuryReport.builder()
                .player(player)
                .team(team)
                .injuryType(request.getInjuryType())
                .description(request.getDescription())
                .status(request.getStatus())
                .reportDate(request.getReportDate())
                .expectedReturnDate(request.getExpectedReturnDate())
                .gamesExpectedToMiss(request.getGamesExpectedToMiss())
                .build();

        log.info("Creating injury report for player id: {}", injuryReport.getPlayer().getId());

        validateInjuryReport(injuryReport);

        return injuryReportRepository.save(injuryReport);
    }

    public InjuryReport updateInjuryReport(Long id, InjuryReportUpdateRequest request) {
        log.info("Updating injury report with id: {}", id);

        InjuryReport existing = getInjuryReportById(id);

        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }
        if (request.getDescription() != null) {
            existing.setDescription(request.getDescription());
        }
        if (request.getExpectedReturnDate() != null) {
            existing.setExpectedReturnDate(request.getExpectedReturnDate());
        }
        if (request.getActualReturnDate() != null) {
            existing.setActualReturnDate(request.getActualReturnDate());
            existing.setIsActive(false); // Deactivate when player returns
        }
        if (request.getGamesExpectedToMiss() != null) {
            existing.setGamesExpectedToMiss(request.getGamesExpectedToMiss());
        }
        if (request.getGamesActuallyMissed() != null) {
            existing.setGamesActuallyMissed(request.getGamesActuallyMissed());
        }
        if (request.getIsActive() != null) {
            existing.setIsActive(request.getIsActive());
        }

        return injuryReportRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public InjuryReport getInjuryReportById(Long id) {
        return injuryReportRepository.findById(id)
                .orElseThrow(() -> new AppException(
                        "Injury report not found with id: " + id,
                        HttpStatus.NOT_FOUND,
                        "INJURY_REPORT_NOT_FOUND"
                ));
    }

    @Transactional(readOnly = true)
    public List<InjuryReport> getInjuryReportsByPlayer(Long playerId) {
        Player player = playerService.getPlayerById(playerId);
        return injuryReportRepository.findByPlayer(player);
    }

    @Transactional(readOnly = true)
    public List<InjuryReport> getActiveInjuryReportsByPlayer(Long playerId) {
        Player player = playerService.getPlayerById(playerId);
        return injuryReportRepository.findByPlayerAndIsActiveTrue(player);
    }

    @Transactional(readOnly = true)
    public List<InjuryReport> getInjuryReportsByTeam(Long teamId) {
        Team team = teamService.getTeamById(teamId);
        return injuryReportRepository.findByTeam(team);
    }

    @Transactional(readOnly = true)
    public List<InjuryReport> getCurrentTeamInjuries(Long teamId) {
        Team team = teamService.getTeamById(teamId);
        return injuryReportRepository.findCurrentTeamInjuries(team);
    }

    @Transactional(readOnly = true)
    public List<InjuryReport> getInjuryReportsByStatus(InjuryStatus status) {
        return injuryReportRepository.findByStatus(status);
    }

    public void deactivateInjuryReport(Long id) {
        log.info("Deactivating injury report with id: {}", id);

        InjuryReport injuryReport = getInjuryReportById(id);
        injuryReport.setIsActive(false);
        injuryReport.setActualReturnDate(LocalDate.now(clock));

        injuryReportRepository.save(injuryReport);
    }

    public void deleteInjuryReport(Long id) {
        log.info("Deleting injury report with id: {}", id);

        if (!injuryReportRepository.existsById(id)) {
            throw new AppException(
                    "Injury report not found with id: " + id,
                    HttpStatus.NOT_FOUND,
                    "INJURY_REPORT_NOT_FOUND"
            );
        }

        injuryReportRepository.deleteById(id);
    }

    private void validateInjuryReport(InjuryReport injuryReport) {
        if (injuryReport.getPlayer() == null) {
            throw new AppException(
                    "Player is required for injury report",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_INJURY_REPORT_DATA"
            );
        }

        if (injuryReport.getTeam() == null) {
            throw new AppException(
                    "Team is required for injury report",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_INJURY_REPORT_DATA"
            );
        }

        if (injuryReport.getInjuryType() == null || injuryReport.getInjuryType().trim().isEmpty()) {
            throw new AppException(
                    "Injury type is required",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_INJURY_REPORT_DATA"
            );
        }
    }
}
