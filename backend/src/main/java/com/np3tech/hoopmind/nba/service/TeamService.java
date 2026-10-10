package com.np3tech.hoopmind.nba.service;

import com.np3tech.hoopmind.nba.dto.TeamFormDTO;
import com.np3tech.hoopmind.nba.entity.Team;
import com.np3tech.hoopmind.nba.entity.enummeration.Conference;
import com.np3tech.hoopmind.nba.entity.enummeration.Division;

import java.util.List;

public interface TeamService {
    Team createTeam(Team team);
    Team updateTeam(Long id, Team teamUpdate);
    Team getTeamById(Long id);
    Team getTeamByAbbreviation(String abbreviation);
    List<Team> getAllTeams();
    List<Team> getTeamsByConference(Conference conference);
    List<Team> getTeamsByDivision(Division division);
    void deleteTeam(Long id);
    TeamFormDTO getTeamForm(Long id, int nbGames);

}
