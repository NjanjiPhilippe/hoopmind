package com.np3tech.hoopmind.nba.service;

import com.np3tech.hoopmind.nba.entity.TeamStats;
import com.np3tech.hoopmind.nba.repository.TeamStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamStatsService {

    private final TeamStatsRepository teamStatsRepository;
    private final SeasonService seasonService;

    public List<TeamStats> getStandingsBySeason(Long seasonId) {
        return teamStatsRepository.findStandingsBySeason(seasonService.getSeasonById(seasonId));
    }
}
