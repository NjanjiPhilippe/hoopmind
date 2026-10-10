package com.np3tech.hoopmind.nba.service;

import com.np3tech.hoopmind.nba.entity.InjuryReport;
import com.np3tech.hoopmind.nba.entity.enummeration.InjuryStatus;

import java.util.List;

public interface InjuryReportService {
    InjuryReport createInjuryReport(InjuryReport injuryReport);
    InjuryReport updateInjuryReport(Long id, InjuryReport injuryUpdate);
    InjuryReport getInjuryReportById(Long id);
    List<InjuryReport> getInjuryReportsByPlayer(Long playerId);
    List<InjuryReport> getActiveInjuryReportsByPlayer(Long playerId);
    List<InjuryReport> getInjuryReportsByTeam(Long teamId);
    List<InjuryReport> getCurrentTeamInjuries(Long teamId);
    List<InjuryReport> getInjuryReportsByStatus(InjuryStatus status);
    void deactivateInjuryReport(Long id);
    void deleteInjuryReport(Long id);
}
