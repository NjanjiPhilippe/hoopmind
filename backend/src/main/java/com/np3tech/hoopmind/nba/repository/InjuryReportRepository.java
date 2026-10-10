package com.np3tech.hoopmind.nba.repository;

import com.np3tech.hoopmind.nba.entity.InjuryReport;
import com.np3tech.hoopmind.nba.entity.Player;
import com.np3tech.hoopmind.nba.entity.Team;
import com.np3tech.hoopmind.nba.entity.enummeration.InjuryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InjuryReportRepository extends JpaRepository<InjuryReport, Long> {

    List<InjuryReport> findByPlayer(Player player);

    List<InjuryReport> findByTeam(Team team);

    List<InjuryReport> findByStatus(InjuryStatus status);

    List<InjuryReport> findByPlayerAndIsActiveTrue(Player player);

    List<InjuryReport> findByTeamAndIsActiveTrue(Team team);

    @Query("SELECT ir FROM InjuryReport ir WHERE ir.isActive = true AND ir.status IN :statuses")
    List<InjuryReport> findActiveByStatuses(@Param("statuses") List<InjuryStatus> statuses);

    List<InjuryReport> findByReportDateBetween(LocalDate start, LocalDate end);

    @Query("SELECT ir FROM InjuryReport ir WHERE ir.team = :team AND ir.isActive = true ORDER BY ir.reportDate DESC")
    List<InjuryReport> findCurrentTeamInjuries(@Param("team") Team team);
}