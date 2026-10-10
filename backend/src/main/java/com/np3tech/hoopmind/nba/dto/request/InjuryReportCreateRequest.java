package com.np3tech.hoopmind.nba.dto.request;

import com.np3tech.hoopmind.nba.entity.enummeration.InjuryStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InjuryReportCreateRequest {
    @NotNull(message = "Player ID is required")
    private Long playerId;

    @NotNull(message = "Team ID is required")
    private Long teamId;

    @NotBlank(message = "Injury type is required")
    private String injuryType;

    private String description;

    @NotNull(message = "Status is required")
    private InjuryStatus status;

    @NotNull(message = "Report date is required")
    private LocalDate reportDate;

    private LocalDate expectedReturnDate;
    private Integer gamesExpectedToMiss;
}