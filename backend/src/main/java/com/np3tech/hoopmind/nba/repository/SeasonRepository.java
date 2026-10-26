package com.np3tech.hoopmind.nba.repository;

import com.np3tech.hoopmind.nba.entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeasonRepository extends JpaRepository<Season, Long> {

    Optional<Season> findByYear(Integer year);

    List<Season> findAllByOrderByYearDesc();

    boolean existsByYear(Integer year);
}
