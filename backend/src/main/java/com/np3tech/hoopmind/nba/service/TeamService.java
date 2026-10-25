package com.np3tech.hoopmind.nba.service;

import com.np3tech.hoopmind.nba.dto.request.TeamCreateRequest;
import com.np3tech.hoopmind.nba.dto.request.TeamUpdateRequest;
import com.np3tech.hoopmind.nba.entity.Team;
import com.np3tech.hoopmind.nba.entity.enummeration.Conference;
import com.np3tech.hoopmind.nba.entity.enummeration.Division;
import com.np3tech.hoopmind.shared.exception.AppException;
import com.np3tech.hoopmind.nba.repository.TeamRepository;
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
        Team team = Team.builder()
                .name(request.getName())
                .city(request.getCity())
                .abbreviation(request.getAbbreviation())
                .conference(request.getConference())
                .division(request.getDivision())
                .logoUrl(request.getLogoUrl())
                .foundedYear(request.getFoundedYear())
                .arena(request.getArena())
                .headCoach(request.getHeadCoach())
                .build();

        log.info("Creating team: {}", team.getName());

        if (teamRepository.existsByAbbreviation(team.getAbbreviation())) {
            throw new AppException(
                    "Team with abbreviation " + team.getAbbreviation() + " already exists",
                    HttpStatus.CONFLICT,
                    "TEAM_ALREADY_EXISTS"
            );
        }

        return teamRepository.save(team);
    }

    public Team updateTeam(Long id, TeamUpdateRequest request) {
        log.info("Updating team with id: {}", id);

        Team existingTeam = getTeamById(id);

        if (request.getName() != null) {
            existingTeam.setName(request.getName());
        }
        if (request.getCity() != null) {
            existingTeam.setCity(request.getCity());
        }
        if (request.getAbbreviation() != null &&
                !request.getAbbreviation().equals(existingTeam.getAbbreviation())) {
            if (teamRepository.existsByAbbreviation(request.getAbbreviation())) {
                throw new AppException(
                        "Team with abbreviation " + request.getAbbreviation() + " already exists",
                        HttpStatus.CONFLICT,
                        "TEAM_ALREADY_EXISTS"
                );
            }
            existingTeam.setAbbreviation(request.getAbbreviation());
        }
        if (request.getConference() != null) {
            existingTeam.setConference(request.getConference());
        }
        if (request.getDivision() != null) {
            existingTeam.setDivision(request.getDivision());
        }
        if (request.getLogoUrl() != null) {
            existingTeam.setLogoUrl(request.getLogoUrl());
        }
        if (request.getArena() != null) {
            existingTeam.setArena(request.getArena());
        }
        if (request.getHeadCoach() != null) {
            existingTeam.setHeadCoach(request.getHeadCoach());
        }

        return teamRepository.save(existingTeam);
    }

    @Transactional(readOnly = true)
    public Team getTeamById(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new AppException(
                        "Team not found with id: " + id,
                        HttpStatus.NOT_FOUND,
                        "TEAM_NOT_FOUND"
                ));
    }

    @Transactional(readOnly = true)
    public Team getTeamByAbbreviation(String abbreviation) {
        return teamRepository.findByAbbreviation(abbreviation)
                .orElseThrow(() -> new AppException(
                        "Team not found with abbreviation: " + abbreviation,
                        HttpStatus.NOT_FOUND,
                        "TEAM_NOT_FOUND"
                ));
    }

    @Transactional(readOnly = true)
    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Team> getTeamsByConference(Conference conference) {
        return teamRepository.findByConference(conference);
    }

    @Transactional(readOnly = true)
    public List<Team> getTeamsByDivision(Division division) {
        return teamRepository.findByDivision(division);
    }

    public void deleteTeam(Long id) {
        log.info("Deleting team with id: {}", id);

        if (!teamRepository.existsById(id)) {
            throw new AppException(
                    "Team not found with id: " + id,
                    HttpStatus.NOT_FOUND,
                    "TEAM_NOT_FOUND"
            );
        }

        teamRepository.deleteById(id);
    }
}
