package com.np3tech.hoopmind.nba.dto.response;

import com.np3tech.hoopmind.nba.entity.enummeration.SeasonStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeasonResponse {
    private Long id;
    private Integer year;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate playoffsStartDate;
    private LocalDate finalsStartDate;
    private SeasonStatus status;
}