package com.np3tech.hoopmind.nba.service.search;

import com.np3tech.hoopmind.nba.entity.Player;
import com.np3tech.hoopmind.nba.entity.enummeration.PlayerStatus;
import com.np3tech.hoopmind.nba.entity.enummeration.Position;
import com.np3tech.hoopmind.nba.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

/**
 * Advanced search and filtering service for players
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PlayerSearchService {

    private final PlayerRepository playerRepository;

    /**
     * Search players with multiple filter criteria
     */
    public List<Player> searchPlayers(
            String searchTerm,
            Long teamId,
            Position position,
            PlayerStatus status,
            Integer minExperience,
            Integer maxExperience,
            String nationality
    ) {
        Specification<Player> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                String searchPattern = "%" + searchTerm.toLowerCase() + "%";
                Predicate firstName = cb.like(cb.lower(root.get("firstName")), searchPattern);
                Predicate lastName = cb.like(cb.lower(root.get("lastName")), searchPattern);
                predicates.add(cb.or(firstName, lastName));
            }

            if (teamId != null) {
                predicates.add(cb.equal(root.get("team").get("id"), teamId));
            }

            if (position != null) {
                predicates.add(cb.equal(root.get("position"), position));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (minExperience != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("experience"), minExperience));
            }

            if (maxExperience != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("experience"), maxExperience));
            }

            if (nationality != null && !nationality.trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("nationality")), nationality.toLowerCase()));
            }

            query.orderBy(cb.asc(root.get("lastName")), cb.asc(root.get("firstName")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return playerRepository.findAll(spec);
    }

    /**
     * Get active players by team
     */
    public List<Player> getActivePlayersByTeam(Long teamId) {
        Specification<Player> spec = (root, query, cb) -> {
            Predicate teamPredicate = cb.equal(root.get("team").get("id"), teamId);
            Predicate statusPredicate = cb.equal(root.get("status"), PlayerStatus.ACTIVE);

            query.orderBy(
                    cb.asc(root.get("position")),
                    cb.asc(root.get("jerseyNumber"))
            );

            return cb.and(teamPredicate, statusPredicate);
        };

        return playerRepository.findAll(spec);
    }

    /**
     * Get rookies (players with 0 experience)
     */
    public List<Player> getRookies() {
        Specification<Player> spec = (root, query, cb) -> {
            Predicate experiencePredicate = cb.or(
                    cb.equal(root.get("experience"), 0),
                    cb.isNull(root.get("experience"))
            );
            Predicate statusPredicate = cb.equal(root.get("status"), PlayerStatus.ACTIVE);

            return cb.and(experiencePredicate, statusPredicate);
        };

        return playerRepository.findAll(spec);
    }

    /**
     * Get veterans (players with significant experience)
     */
    public List<Player> getVeterans(int minExperience) {
        Specification<Player> spec = (root, query, cb) -> {
            Predicate experiencePredicate = cb.greaterThanOrEqualTo(root.get("experience"), minExperience);
            Predicate statusPredicate = cb.equal(root.get("status"), PlayerStatus.ACTIVE);

            query.orderBy(cb.desc(root.get("experience")));

            return cb.and(experiencePredicate, statusPredicate);
        };

        return playerRepository.findAll(spec);
    }

    /**
     * Get players from a specific college
     */
    public List<Player> getPlayersByCollege(String college) {
        Specification<Player> spec = (root, query, cb) -> {
            return cb.equal(cb.lower(root.get("college")), college.toLowerCase());
        };

        return playerRepository.findAll(spec);
    }

    /**
     * Get international players (non-USA)
     */
    public List<Player> getInternationalPlayers() {
        Specification<Player> spec = (root, query, cb) -> {
            Predicate notUSA = cb.notEqual(cb.lower(root.get("nationality")), "usa");
            Predicate notNull = cb.isNotNull(root.get("nationality"));
            Predicate statusPredicate = cb.equal(root.get("status"), PlayerStatus.ACTIVE);

            query.orderBy(cb.asc(root.get("nationality")), cb.asc(root.get("lastName")));

            return cb.and(notUSA, notNull, statusPredicate);
        };

        return playerRepository.findAll(spec);
    }
}
