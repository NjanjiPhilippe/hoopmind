package com.np3tech.hoopmind.nba.service;

import com.np3tech.hoopmind.nba.dto.request.SeasonCreateRequest;
import com.np3tech.hoopmind.nba.dto.request.SeasonUpdateRequest;
import com.np3tech.hoopmind.nba.entity.Season;
import com.np3tech.hoopmind.nba.repository.SeasonRepository;
import com.np3tech.hoopmind.shared.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SeasonService {

    private final SeasonRepository seasonRepository;

    public Season createSeason(SeasonCreateRequest request) {
        if (seasonRepository.existsByYear(request.getYear())) {
            throw new AppException("Season with year " + request.getYear() + " already exists",
                    HttpStatus.CONFLICT, "SEASON_ALREADY_EXISTS");
        }
        log.info("Creating season: {}", request.getName());
        return seasonRepository.save(Season.builder()
                .year(request.getYear())
                .name(request.getName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .playoffsStartDate(request.getPlayoffsStartDate())
                .finalsStartDate(request.getFinalsStartDate())
                .status(request.getStatus())
                .build());
    }

    public Season updateSeason(Long id, SeasonUpdateRequest request) {
        Season season = getSeasonById(id);
        if (request.getStatus() != null) {
            season.setStatus(request.getStatus());
        }
        if (request.getStartDate() != null) {
            season.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            season.setEndDate(request.getEndDate());
        }
        if (request.getPlayoffsStartDate() != null) {
            season.setPlayoffsStartDate(request.getPlayoffsStartDate());
        }
        if (request.getFinalsStartDate() != null) {
            season.setFinalsStartDate(request.getFinalsStartDate());
        }
        return seasonRepository.save(season);
    }

    @Transactional(readOnly = true)
    public Season getSeasonById(Long id) {
        return seasonRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("SEASON_NOT_FOUND", "Season not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Season> getAllSeasons() {
        return seasonRepository.findAllByOrderByYearDesc();
    }

    public void deleteSeason(Long id) {
        log.info("Deleting season with id: {}", id);
        seasonRepository.delete(getSeasonById(id));
    }
}
