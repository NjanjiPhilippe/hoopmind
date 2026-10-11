package com.np3tech.hoopmind.nba.repository;

import com.np3tech.hoopmind.nba.entity.Team;
import com.np3tech.hoopmind.nba.entity.enummeration.Conference;
import com.np3tech.hoopmind.nba.entity.enummeration.Division;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    Optional<Team> findByAbbreviation(String abbreviation);

    Optional<Team> findByExternalId(Long externalId);

    Optional<Team> findByName(String name);

    List<Team> findByConference(Conference conference);

    List<Team> findByDivision(Division division);

    List<Team> findByCityContainingIgnoreCase(String city);

    boolean existsByAbbreviation(String abbreviation);
}