package com.np3tech.hoopmind.nba.dto.response;

import com.np3tech.hoopmind.nba.entity.enummeration.InjuryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InjuryReportResponse {
    private Long id;
    private PlayerSummaryResponse player;
    private TeamSummaryResponse team;
    private String injuryType;
    private String description;
    private InjuryStatus status;
    private LocalDate reportDate;
    private LocalDate expectedReturnDate;
    private LocalDate actualReturnDate;
    private Integer gamesExpectedToMiss;
    private Integer gamesActuallyMissed;
    private Boolean isActive;
}