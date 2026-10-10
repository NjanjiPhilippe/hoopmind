package com.np3tech.hoopmind.nba.service;

import com.np3tech.hoopmind.nba.entity.Season;
import com.np3tech.hoopmind.nba.entity.enummeration.SeasonStatus;

import java.util.List;

public interface SeasonService {
    Season createSeason(Season season);
    Season updateSeason(Long id, Season seasonUpdate);
    Season getSeasonById(Long id);
    Season getSeasonByYear(Integer year);
    Season getActiveSeason();
    List<Season> getAllSeasons();
    List<Season> getSeasonsByStatus(SeasonStatus status);
    void updateSeasonStatus(Long id, SeasonStatus status);
    void deleteSeason(Long id);
}
