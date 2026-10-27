package com.np3tech.hoopmind.nba.service;

import com.np3tech.hoopmind.nba.dto.request.TeamCreateRequest;
import com.np3tech.hoopmind.nba.dto.request.TeamUpdateRequest;
import com.np3tech.hoopmind.nba.entity.Team;
import com.np3tech.hoopmind.nba.repository.TeamRepository;
import com.np3tech.hoopmind.shared.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TeamService {

    private final TeamRepository teamRepository;

    public Team createTeam(TeamCreateRequest request) {
        requireFreeAbbreviation(request.getAbbreviation());
        log.info("Creating team: {}", request.getName());
        return teamRepository.save(Team.builder()
                .name(request.getName())
                .city(request.getCity())
                .abbreviation(request.getAbbreviation())
                .conference(request.getConference())
                .division(request.getDivision())
                .logoUrl(request.getLogoUrl())
                .foundedYear(request.getFoundedYear())
                .arena(request.getArena())
                .headCoach(request.getHeadCoach())
                .build());
    }

    public Team updateTeam(Long id, TeamUpdateRequest request) {
        Team team = getTeamById(id);
        if (request.getName() != null) {
            team.setName(request.getName());
        }
        if (request.getCity() != null) {
            team.setCity(request.getCity());
        }
        if (request.getAbbreviation() != null && !request.getAbbreviation().equals(team.getAbbreviation())) {
            requireFreeAbbreviation(request.getAbbreviation());
            team.setAbbreviation(request.getAbbreviation());
        }
        if (request.getConference() != null) {
            team.setConference(request.getConference());
        }
        if (request.getDivision() != null) {
            team.setDivision(request.getDivision());
        }
        if (request.getLogoUrl() != null) {
            team.setLogoUrl(request.getLogoUrl());
        }
        if (request.getArena() != null) {
            team.setArena(request.getArena());
        }
        if (request.getHeadCoach() != null) {
            team.setHeadCoach(request.getHeadCoach());
        }
        return teamRepository.save(team);
    }

    @Transactional(readOnly = true)
    public Team getTeamById(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("TEAM_NOT_FOUND", "Team not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    public void deleteTeam(Long id) {
        log.info("Deleting team with id: {}", id);
        teamRepository.delete(getTeamById(id));
    }

    private void requireFreeAbbreviation(String abbreviation) {
        if (teamRepository.existsByAbbreviation(abbreviation)) {
            throw new AppException("Team with abbreviation " + abbreviation + " already exists",
                    HttpStatus.CONFLICT, "TEAM_ALREADY_EXISTS");
        }
    }
}
