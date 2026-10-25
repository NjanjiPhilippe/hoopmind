package com.np3tech.hoopmind.nba.repository;

import com.np3tech.hoopmind.nba.entity.InjuryReport;
import com.np3tech.hoopmind.nba.entity.Player;
import com.np3tech.hoopmind.nba.entity.Team;
import com.np3tech.hoopmind.nba.entity.enummeration.InjuryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Les lectures chargent le joueur et l'équipe avec le rapport (réponse construite hors transaction). */
@Repository
public interface InjuryReportRepository extends JpaRepository<InjuryReport, Long> {

    @Override
    @EntityGraph(attributePaths = {"player", "team"})
    Optional<InjuryReport> findById(Long id);

    @EntityGraph(attributePaths = {"player", "team"})
    List<InjuryReport> findByPlayer(Player player);

    @EntityGraph(attributePaths = {"player", "team"})
    List<InjuryReport> findByTeam(Team team);

    @EntityGraph(attributePaths = {"player", "team"})
    List<InjuryReport> findByStatus(InjuryStatus status);

    @EntityGraph(attributePaths = {"player", "team"})
    List<InjuryReport> findByPlayerAndIsActiveTrue(Player player);

    @EntityGraph(attributePaths = {"player", "team"})
    List<InjuryReport> findByTeamAndIsActiveTrue(Team team);

    @EntityGraph(attributePaths = {"player", "team"})
    @Query("SELECT ir FROM InjuryReport ir WHERE ir.isActive = true AND ir.status IN :statuses")
    List<InjuryReport> findActiveByStatuses(@Param("statuses") List<InjuryStatus> statuses);

    @EntityGraph(attributePaths = {"player", "team"})
    List<InjuryReport> findByReportDateBetween(LocalDate start, LocalDate end);

    @EntityGraph(attributePaths = {"player", "team"})
    @Query("SELECT ir FROM InjuryReport ir WHERE ir.team = :team AND ir.isActive = true ORDER BY ir.reportDate DESC")
    List<InjuryReport> findCurrentTeamInjuries(@Param("team") Team team);
}