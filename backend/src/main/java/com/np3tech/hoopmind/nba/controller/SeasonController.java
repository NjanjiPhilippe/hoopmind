package com.np3tech.hoopmind.nba.controller;

import com.np3tech.hoopmind.nba.api.SeasonApi;
import com.np3tech.hoopmind.nba.mapper.SeasonMapper;
import com.np3tech.hoopmind.nba.dto.request.SeasonCreateRequest;
import com.np3tech.hoopmind.nba.dto.request.SeasonUpdateRequest;
import com.np3tech.hoopmind.nba.dto.response.SeasonResponse;
import com.np3tech.hoopmind.nba.entity.Season;
import com.np3tech.hoopmind.nba.entity.enummeration.SeasonStatus;
import com.np3tech.hoopmind.nba.service.SeasonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SeasonController implements SeasonApi {

    private final SeasonService seasonService;
    private final SeasonMapper seasonMapper;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SeasonResponse> createSeason(SeasonCreateRequest request) {
        log.info("Creating season: {}", request.getName());

        Season season = seasonMapper.toEntity(request);
        Season createdSeason = seasonService.createSeason(season);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(seasonMapper.toResponse(createdSeason));
    }

    @Override
    public ResponseEntity<SeasonResponse> getSeasonById(Long id) {
        log.debug("Fetching season with id: {}", id);

        Season season = seasonService.getSeasonById(id);
        return ResponseEntity.ok(seasonMapper.toResponse(season));
    }

    @Override
    public ResponseEntity<List<SeasonResponse>> getAllSeasons() {
        log.debug("Fetching all seasons");

        List<Season> seasons = seasonService.getAllSeasons();
        List<SeasonResponse> responses = seasons.stream()
                .map(seasonMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<SeasonResponse> getActiveSeason() {
        log.debug("Fetching active season");

        Season season = seasonService.getActiveSeason();
        return ResponseEntity.ok(seasonMapper.toResponse(season));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SeasonResponse> updateSeason(Long id, SeasonUpdateRequest request) {
        log.info("Updating season with id: {}", id);

        Season seasonUpdate = seasonMapper.toEntity(request);
        Season updatedSeason = seasonService.updateSeason(id, seasonUpdate);

        return ResponseEntity.ok(seasonMapper.toResponse(updatedSeason));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSeason(Long id) {
        log.info("Deleting season with id: {}", id);

        seasonService.deleteSeason(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SeasonResponse> updateSeasonStatus(Long id, SeasonStatus status) {
        log.info("Updating season status for id: {} to {}", id, status);

        seasonService.updateSeasonStatus(id, status);
        Season season = seasonService.getSeasonById(id);

        return ResponseEntity.ok(seasonMapper.toResponse(season));
    }
}