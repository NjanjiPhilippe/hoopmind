package com.np3tech.hoopmind.nba.repository;

import com.np3tech.hoopmind.nba.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    Optional<Team> findByAbbreviation(String abbreviation);

    Optional<Team> findByExternalId(Long externalId);

    boolean existsByAbbreviation(String abbreviation);
}
