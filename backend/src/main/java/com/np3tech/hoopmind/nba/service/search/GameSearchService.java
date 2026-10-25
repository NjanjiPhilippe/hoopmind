package com.np3tech.hoopmind.nba.service.search;

import com.np3tech.hoopmind.nba.entity.Game;
import com.np3tech.hoopmind.nba.entity.Season;
import com.np3tech.hoopmind.nba.entity.Team;
import com.np3tech.hoopmind.nba.entity.enummeration.GameStatus;
import com.np3tech.hoopmind.nba.entity.enummeration.GameType;
import com.np3tech.hoopmind.nba.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Advanced search and filtering service for games
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GameSearchService {
    private final GameRepository gameRepository;
    private final Clock clock;

    /**
     * Search games with multiple filter criteria
     */
    public List<Game> searchGames(
            Long seasonId,
            Long teamId,
            GameStatus status,
            GameType gameType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Boolean isNationalTv
    ) {
        Specification<Game> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (seasonId != null) {
                predicates.add(cb.equal(root.get("season").get("id"), seasonId));
            }

            if (teamId != null) {
                Predicate homeTeam = cb.equal(root.get("homeTeam").get("id"), teamId);
                Predicate awayTeam = cb.equal(root.get("awayTeam").get("id"), teamId);
                predicates.add(cb.or(homeTeam, awayTeam));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (gameType != null) {
                predicates.add(cb.equal(root.get("gameType"), gameType));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("scheduledDate"), startDate));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("scheduledDate"), endDate));
            }

            if (isNationalTv != null) {
                predicates.add(cb.equal(root.get("isNationalTv"), isNationalTv));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return gameRepository.findAll(spec);
    }

    /**
     * Get upcoming games for a team
     */
    public List<Game> getUpcomingGamesForTeam(Long teamId, int limit) {
        LocalDateTime now = LocalDateTime.now(clock);

        Specification<Game> spec = (root, query, cb) -> {
            Predicate teamPredicate = cb.or(
                    cb.equal(root.get("homeTeam").get("id"), teamId),
                    cb.equal(root.get("awayTeam").get("id"), teamId)
            );

            Predicate datePredicate = cb.greaterThanOrEqualTo(root.get("scheduledDate"), now);
            Predicate statusPredicate = cb.equal(root.get("status"), GameStatus.SCHEDULED);

            query.orderBy(cb.asc(root.get("scheduledDate")));

            return cb.and(teamPredicate, datePredicate, statusPredicate);
        };

        List<Game> games = gameRepository.findAll(spec);
        return games.size() > limit ? games.subList(0, limit) : games;
    }

    /**
     * Get recent games for a team
     */
    public List<Game> getRecentGamesForTeam(Long teamId, int limit) {
        LocalDateTime now = LocalDateTime.now(clock);

        Specification<Game> spec = (root, query, cb) -> {
            Predicate teamPredicate = cb.or(
                    cb.equal(root.get("homeTeam").get("id"), teamId),
                    cb.equal(root.get("awayTeam").get("id"), teamId)
            );

            Predicate datePredicate = cb.lessThan(root.get("scheduledDate"), now);
            Predicate statusPredicate = cb.equal(root.get("status"), GameStatus.COMPLETED);

            query.orderBy(cb.desc(root.get("scheduledDate")));

            return cb.and(teamPredicate, datePredicate, statusPredicate);
        };

        List<Game> games = gameRepository.findAll(spec);
        return games.size() > limit ? games.subList(0, limit) : games;
    }

    /**
     * Get games scheduled for today
     */
    public List<Game> getTodaysGames() {
        LocalDateTime startOfDay = LocalDateTime.now(clock).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now(clock).withHour(23).withMinute(59).withSecond(59);

        return gameRepository.findByScheduledDateBetween(startOfDay, endOfDay);
    }

    /**
     * Get live games (currently in progress)
     */
    public List<Game> getLiveGames() {
        return gameRepository.findByStatus(GameStatus.IN_PROGRESS);
    }

    /**
     * Get national TV games for a date range
     */
    public List<Game> getNationalTVGames(LocalDateTime startDate, LocalDateTime endDate) {
        Specification<Game> spec = (root, query, cb) -> {
            Predicate tvPredicate = cb.equal(root.get("isNationalTv"), true);
            Predicate datePredicate = cb.between(root.get("scheduledDate"), startDate, endDate);

            query.orderBy(cb.asc(root.get("scheduledDate")));

            return cb.and(tvPredicate, datePredicate);
        };

        return gameRepository.findAll(spec);
    }
}
