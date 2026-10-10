package com.np3tech.hoopmind.nba.service.impl;

import com.np3tech.hoopmind.nba.dto.TeamFormDTO;
import com.np3tech.hoopmind.nba.entity.Team;
import com.np3tech.hoopmind.nba.entity.enummeration.Conference;
import com.np3tech.hoopmind.nba.entity.enummeration.Division;
import com.np3tech.hoopmind.shared.exception.AppException;
import com.np3tech.hoopmind.nba.repository.TeamRepository;
import com.np3tech.hoopmind.nba.service.TeamService;
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
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;

    public Team createTeam(Team team) {
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

    public Team updateTeam(Long id, Team teamUpdate) {
        log.info("Updating team with id: {}", id);

        Team existingTeam = getTeamById(id);

        if (teamUpdate.getName() != null) {
            existingTeam.setName(teamUpdate.getName());
        }
        if (teamUpdate.getCity() != null) {
            existingTeam.setCity(teamUpdate.getCity());
        }
        if (teamUpdate.getAbbreviation() != null &&
                !teamUpdate.getAbbreviation().equals(existingTeam.getAbbreviation())) {
            if (teamRepository.existsByAbbreviation(teamUpdate.getAbbreviation())) {
                throw new AppException(
                        "Team with abbreviation " + teamUpdate.getAbbreviation() + " already exists",
                        HttpStatus.CONFLICT,
                        "TEAM_ALREADY_EXISTS"
                );
            }
            existingTeam.setAbbreviation(teamUpdate.getAbbreviation());
        }
        if (teamUpdate.getConference() != null) {
            existingTeam.setConference(teamUpdate.getConference());
        }
        if (teamUpdate.getDivision() != null) {
            existingTeam.setDivision(teamUpdate.getDivision());
        }
        if (teamUpdate.getLogoUrl() != null) {
            existingTeam.setLogoUrl(teamUpdate.getLogoUrl());
        }
        if (teamUpdate.getArena() != null) {
            existingTeam.setArena(teamUpdate.getArena());
        }
        if (teamUpdate.getHeadCoach() != null) {
            existingTeam.setHeadCoach(teamUpdate.getHeadCoach());
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

    @Override
    public TeamFormDTO getTeamForm(Long id, int nbGames) {
        return null;
    }
}
