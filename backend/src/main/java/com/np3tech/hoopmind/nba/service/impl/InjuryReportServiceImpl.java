package com.np3tech.hoopmind.nba.service.impl;

import com.np3tech.hoopmind.nba.entity.InjuryReport;
import com.np3tech.hoopmind.nba.entity.Player;
import com.np3tech.hoopmind.nba.entity.Team;
import com.np3tech.hoopmind.nba.entity.enummeration.InjuryStatus;
import com.np3tech.hoopmind.shared.exception.AppException;
import com.np3tech.hoopmind.nba.repository.InjuryReportRepository;
import com.np3tech.hoopmind.nba.service.InjuryReportService;
import com.np3tech.hoopmind.nba.service.PlayerService;
import com.np3tech.hoopmind.nba.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InjuryReportServiceImpl implements InjuryReportService {

    private final InjuryReportRepository injuryReportRepository;
    private final PlayerService playerService;
    private final TeamService teamService;

    public InjuryReport createInjuryReport(InjuryReport injuryReport) {
        log.info("Creating injury report for player id: {}", injuryReport.getPlayer().getId());

        validateInjuryReport(injuryReport);

        return injuryReportRepository.save(injuryReport);
    }

    public InjuryReport updateInjuryReport(Long id, InjuryReport injuryUpdate) {
        log.info("Updating injury report with id: {}", id);

        InjuryReport existing = getInjuryReportById(id);

        if (injuryUpdate.getStatus() != null) {
            existing.setStatus(injuryUpdate.getStatus());
        }
        if (injuryUpdate.getDescription() != null) {
            existing.setDescription(injuryUpdate.getDescription());
        }
        if (injuryUpdate.getExpectedReturnDate() != null) {
            existing.setExpectedReturnDate(injuryUpdate.getExpectedReturnDate());
        }
        if (injuryUpdate.getActualReturnDate() != null) {
            existing.setActualReturnDate(injuryUpdate.getActualReturnDate());
            existing.setIsActive(false); // Deactivate when player returns
        }
        if (injuryUpdate.getGamesExpectedToMiss() != null) {
            existing.setGamesExpectedToMiss(injuryUpdate.getGamesExpectedToMiss());
        }
        if (injuryUpdate.getGamesActuallyMissed() != null) {
            existing.setGamesActuallyMissed(injuryUpdate.getGamesActuallyMissed());
        }
        if (injuryUpdate.getIsActive() != null) {
            existing.setIsActive(injuryUpdate.getIsActive());
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
        injuryReport.setActualReturnDate(LocalDate.now());

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
