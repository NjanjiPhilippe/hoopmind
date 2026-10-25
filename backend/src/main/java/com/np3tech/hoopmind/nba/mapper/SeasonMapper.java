package com.np3tech.hoopmind.nba.mapper;

import com.np3tech.hoopmind.nba.dto.response.SeasonResponse;
import com.np3tech.hoopmind.nba.entity.Season;
import org.springframework.stereotype.Component;

@Component
public class SeasonMapper {

    public SeasonResponse toResponse(Season season) {
        return SeasonResponse.builder()
                .id(season.getId())
                .year(season.getYear())
                .name(season.getName())
                .startDate(season.getStartDate())
                .endDate(season.getEndDate())
                .playoffsStartDate(season.getPlayoffsStartDate())
                .finalsStartDate(season.getFinalsStartDate())
                .status(season.getStatus())
                .build();
    }
}