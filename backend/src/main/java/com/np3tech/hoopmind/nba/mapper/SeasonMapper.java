package com.np3tech.hoopmind.nba.mapper;

import com.np3tech.hoopmind.nba.dto.request.SeasonCreateRequest;
import com.np3tech.hoopmind.nba.dto.request.SeasonUpdateRequest;
import com.np3tech.hoopmind.nba.dto.response.SeasonResponse;
import com.np3tech.hoopmind.nba.entity.Season;
import org.springframework.stereotype.Component;

@Component
public class SeasonMapper {

    public Season toEntity(SeasonCreateRequest request) {
        return Season.builder()
                .year(request.getYear())
                .name(request.getName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .playoffsStartDate(request.getPlayoffsStartDate())
                .finalsStartDate(request.getFinalsStartDate())
                .status(request.getStatus())
                .build();
    }

    public Season toEntity(SeasonUpdateRequest request) {
        return Season.builder()
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .playoffsStartDate(request.getPlayoffsStartDate())
                .finalsStartDate(request.getFinalsStartDate())
                .status(request.getStatus())
                .build();
    }

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