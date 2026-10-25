package com.np3tech.hoopmind.nba.controller;

import com.np3tech.hoopmind.nba.api.TeamApi;
import com.np3tech.hoopmind.nba.dto.request.TeamCreateRequest;
import com.np3tech.hoopmind.nba.dto.request.TeamUpdateRequest;
import com.np3tech.hoopmind.nba.dto.response.TeamResponse;
import com.np3tech.hoopmind.nba.entity.Team;
import com.np3tech.hoopmind.nba.entity.enummeration.Conference;
import com.np3tech.hoopmind.nba.entity.enummeration.Division;
import com.np3tech.hoopmind.nba.mapper.TeamMapper;
import com.np3tech.hoopmind.nba.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TeamController implements TeamApi {

    private final TeamService teamService;
    private final TeamMapper teamMapper;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeamResponse> createTeam(TeamCreateRequest request) {
        log.info("Creating team: {}", request.getName());
        Team createdTeam = teamService.createTeam(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teamMapper.toResponse(createdTeam));
    }

    @Override
    public ResponseEntity<TeamResponse> getTeamById(Long id) {
        log.debug("Fetching team with id: {}", id);

        Team team = teamService.getTeamById(id);
        return ResponseEntity.ok(teamMapper.toResponse(team));
    }

    @Override
    public ResponseEntity<List<TeamResponse>> getAllTeams() {
        log.debug("Fetching all teams");

        List<Team> teams = teamService.getAllTeams();
        List<TeamResponse> responses = teams.stream()
                .map(teamMapper::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeamResponse> updateTeam(Long id, TeamUpdateRequest request) {
        log.info("Updating team with id: {}", id);
        Team updatedTeam = teamService.updateTeam(id, request);

        return ResponseEntity.ok(teamMapper.toResponse(updatedTeam));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTeam(Long id) {
        log.info("Deleting team with id: {}", id);

        teamService.deleteTeam(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<TeamResponse>> getTeamsByConference(Conference conference) {
        log.debug("Fetching teams by conference: {}", conference);

        List<Team> teams = teamService.getTeamsByConference(conference);
        List<TeamResponse> responses = teams.stream()
                .map(teamMapper::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<List<TeamResponse>> getTeamsByDivision(Division division) {
        log.debug("Fetching teams by division: {}", division);

        List<Team> teams = teamService.getTeamsByDivision(division);
        List<TeamResponse> responses = teams.stream()
                .map(teamMapper::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<TeamResponse> getTeamByAbbreviation(String abbreviation) {
        log.debug("Fetching team by abbreviation: {}", abbreviation);

        Team team = teamService.getTeamByAbbreviation(abbreviation);
        return ResponseEntity.ok(teamMapper.toResponse(team));
    }
}