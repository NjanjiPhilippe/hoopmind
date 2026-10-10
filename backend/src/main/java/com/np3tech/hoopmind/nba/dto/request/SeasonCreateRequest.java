package com.np3tech.hoopmind.nba.dto.request;

import com.np3tech.hoopmind.nba.entity.enummeration.SeasonStatus;
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
public class SeasonCreateRequest {
    @NotNull(message = "Year is required")
    private Integer year;

    @NotBlank(message = "Season name is required")
    private String name;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private LocalDate playoffsStartDate;
    private LocalDate finalsStartDate;

    @NotNull(message = "Status is required")
    private SeasonStatus status;
}
