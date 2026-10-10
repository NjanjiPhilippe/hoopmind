package com.np3tech.hoopmind.nba.controller;

import com.np3tech.hoopmind.nba.api.InjuryReportApi;
import com.np3tech.hoopmind.nba.mapper.InjuryReportMapper;
import com.np3tech.hoopmind.nba.dto.request.InjuryReportCreateRequest;
import com.np3tech.hoopmind.nba.dto.request.InjuryReportUpdateRequest;
import com.np3tech.hoopmind.nba.dto.response.InjuryReportResponse;
import com.np3tech.hoopmind.nba.entity.InjuryReport;
import com.np3tech.hoopmind.nba.entity.enummeration.InjuryStatus;
import com.np3tech.hoopmind.nba.service.InjuryReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class InjuryReportController implements InjuryReportApi {

    private final InjuryReportService injuryReportService;
    private final InjuryReportMapper injuryReportMapper;

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<InjuryReportResponse> createInjuryReport(InjuryReportCreateRequest request) {
        log.info("Creating injury report for player: {}", request.getPlayerId());

        InjuryReport injuryReport = injuryReportMapper.toEntity(request);
        InjuryReport created = injuryReportService.createInjuryReport(injuryReport);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(injuryReportMapper.toResponse(created));
    }

    @Override
    public ResponseEntity<InjuryReportResponse> getInjuryReportById(Long id) {
        log.debug("Fetching injury report with id: {}", id);

        InjuryReport injuryReport = injuryReportService.getInjuryReportById(id);
        return ResponseEntity.ok(injuryReportMapper.toResponse(injuryReport));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<InjuryReportResponse> updateInjuryReport(Long id, InjuryReportUpdateRequest request) {
        log.info("Updating injury report with id: {}", id);

        InjuryReport injuryUpdate = injuryReportMapper.toEntity(request);
        InjuryReport updated = injuryReportService.updateInjuryReport(id, injuryUpdate);

        return ResponseEntity.ok(injuryReportMapper.toResponse(updated));
    }

    @Override
    public ResponseEntity<List<InjuryReportResponse>> getInjuryReportsByPlayer(Long playerId) {
        log.debug("Fetching injury reports for player: {}", playerId);

        List<InjuryReport> reports = injuryReportService.getInjuryReportsByPlayer(playerId);
        List<InjuryReportResponse> responses = reports.stream()
                .map(injuryReportMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<List<InjuryReportResponse>> getActiveInjuryReportsByPlayer(Long playerId) {
        log.debug("Fetching active injury reports for player: {}", playerId);

        List<InjuryReport> reports = injuryReportService.getActiveInjuryReportsByPlayer(playerId);
        List<InjuryReportResponse> responses = reports.stream()
                .map(injuryReportMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<List<InjuryReportResponse>> getInjuryReportsByTeam(Long teamId) {
        log.debug("Fetching injury reports for team: {}", teamId);

        List<InjuryReport> reports = injuryReportService.getInjuryReportsByTeam(teamId);
        List<InjuryReportResponse> responses = reports.stream()
                .map(injuryReportMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<List<InjuryReportResponse>> getCurrentTeamInjuries(Long teamId) {
        log.debug("Fetching current injuries for team: {}", teamId);

        List<InjuryReport> reports = injuryReportService.getCurrentTeamInjuries(teamId);
        List<InjuryReportResponse> responses = reports.stream()
                .map(injuryReportMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<List<InjuryReportResponse>> getInjuryReportsByStatus(InjuryStatus status) {
        log.debug("Fetching injury reports by status: {}", status);

        List<InjuryReport> reports = injuryReportService.getInjuryReportsByStatus(status);
        List<InjuryReportResponse> responses = reports.stream()
                .map(injuryReportMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<Void> deactivateInjuryReport(Long id) {
        log.info("Deactivating injury report with id: {}", id);

        injuryReportService.deactivateInjuryReport(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteInjuryReport(Long id) {
        log.info("Deleting injury report with id: {}", id);

        injuryReportService.deleteInjuryReport(id);
        return ResponseEntity.noContent().build();
    }
}
