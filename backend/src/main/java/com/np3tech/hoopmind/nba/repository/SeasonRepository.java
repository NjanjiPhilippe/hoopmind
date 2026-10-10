package com.np3tech.hoopmind.nba.repository;

import com.np3tech.hoopmind.nba.entity.Season;
import com.np3tech.hoopmind.nba.entity.enummeration.SeasonStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeasonRepository extends JpaRepository<Season, Long> {

    Optional<Season> findByYear(Integer year);

    Optional<Season> findByName(String name);

    List<Season> findByStatus(SeasonStatus status);

    @Query("SELECT s FROM Season s WHERE s.status = 'ACTIVE' ORDER BY s.year DESC")
    Optional<Season> findActiveSeason();

    @Query("SELECT s FROM Season s ORDER BY s.year DESC")
    List<Season> findAllOrderByYearDesc();

    boolean existsByYear(Integer year);
}
