package com.np3tech.hoopmind.nba.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeasonSummaryResponse {
    private Long id;
    private Integer year;
    private String name;
}

