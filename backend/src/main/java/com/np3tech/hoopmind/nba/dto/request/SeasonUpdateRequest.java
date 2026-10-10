package com.np3tech.hoopmind.nba.dto.request;

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
public class SeasonUpdateRequest {
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate playoffsStartDate;
    private LocalDate finalsStartDate;
    private SeasonStatus status;
}