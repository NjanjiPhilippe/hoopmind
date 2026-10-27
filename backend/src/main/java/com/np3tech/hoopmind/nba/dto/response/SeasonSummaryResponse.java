package com.np3tech.hoopmind.nba.dto.response;

import com.np3tech.hoopmind.nba.entity.Season;

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

    public static SeasonSummaryResponse of(Season season) {
        return new SeasonSummaryResponse(season.getId(), season.getYear(), season.getName());
    }
}
