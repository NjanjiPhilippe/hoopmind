package com.np3tech.hoopmind.nba.service.impl;

import com.np3tech.hoopmind.nba.entity.Season;
import com.np3tech.hoopmind.nba.entity.enummeration.SeasonStatus;
import com.np3tech.hoopmind.shared.exception.AppException;
import com.np3tech.hoopmind.nba.repository.SeasonRepository;
import com.np3tech.hoopmind.nba.service.SeasonService;
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
public class SeasonServiceImpl implements SeasonService {

    private final SeasonRepository seasonRepository;

    public Season createSeason(Season season) {
        log.info("Creating season: {}", season.getName());

        if (seasonRepository.existsByYear(season.getYear())) {
            throw new AppException(
                    "Season with year " + season.getYear() + " already exists",
                    HttpStatus.CONFLICT,
                    "SEASON_ALREADY_EXISTS"
            );
        }

        return seasonRepository.save(season);
    }

    public Season updateSeason(Long id, Season seasonUpdate) {
        log.info("Updating season with id: {}", id);

        Season existingSeason = getSeasonById(id);

        if (seasonUpdate.getStatus() != null) {
            existingSeason.setStatus(seasonUpdate.getStatus());
        }
        if (seasonUpdate.getStartDate() != null) {
            existingSeason.setStartDate(seasonUpdate.getStartDate());
        }
        if (seasonUpdate.getEndDate() != null) {
            existingSeason.setEndDate(seasonUpdate.getEndDate());
        }
        if (seasonUpdate.getPlayoffsStartDate() != null) {
            existingSeason.setPlayoffsStartDate(seasonUpdate.getPlayoffsStartDate());
        }
        if (seasonUpdate.getFinalsStartDate() != null) {
            existingSeason.setFinalsStartDate(seasonUpdate.getFinalsStartDate());
        }

        return seasonRepository.save(existingSeason);
    }

    @Transactional(readOnly = true)
    public Season getSeasonById(Long id) {
        return seasonRepository.findById(id)
                .orElseThrow(() -> new AppException(
                        "Season not found with id: " + id,
                        HttpStatus.NOT_FOUND,
                        "SEASON_NOT_FOUND"
                ));
    }

    @Transactional(readOnly = true)
    public Season getSeasonByYear(Integer year) {
        return seasonRepository.findByYear(year)
                .orElseThrow(() -> new AppException(
                        "Season not found with year: " + year,
                        HttpStatus.NOT_FOUND,
                        "SEASON_NOT_FOUND"
                ));
    }

    @Transactional(readOnly = true)
    public Season getActiveSeason() {
        return seasonRepository.findActiveSeason()
                .orElseThrow(() -> new AppException(
                        "No active season found",
                        HttpStatus.NOT_FOUND,
                        "ACTIVE_SEASON_NOT_FOUND"
                ));
    }

    @Transactional(readOnly = true)
    public List<Season> getAllSeasons() {
        return seasonRepository.findAllOrderByYearDesc();
    }

    @Transactional(readOnly = true)
    public List<Season> getSeasonsByStatus(SeasonStatus status) {
        return seasonRepository.findByStatus(status);
    }

    public void updateSeasonStatus(Long id, SeasonStatus status) {
        log.info("Updating season status for id: {} to {}", id, status);

        Season season = getSeasonById(id);
        season.setStatus(status);

        seasonRepository.save(season);
    }

    public void deleteSeason(Long id) {
        log.info("Deleting season with id: {}", id);

        if (!seasonRepository.existsById(id)) {
            throw new AppException(
                    "Season not found with id: " + id,
                    HttpStatus.NOT_FOUND,
                    "SEASON_NOT_FOUND"
            );
        }

        seasonRepository.deleteById(id);
    }
}