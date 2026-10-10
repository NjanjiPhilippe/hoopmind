package com.np3tech.hoopmind.nba.dto.request;

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
public class InjuryReportUpdateRequest {
    private InjuryStatus status;
    private String description;
    private LocalDate expectedReturnDate;
    private LocalDate actualReturnDate;
    private Integer gamesExpectedToMiss;
    private Integer gamesActuallyMissed;
    private Boolean isActive;
}