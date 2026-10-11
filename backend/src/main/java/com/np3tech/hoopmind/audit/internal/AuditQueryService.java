package com.np3tech.hoopmind.audit.internal;

import com.np3tech.hoopmind.audit.AuditAction;
import com.np3tech.hoopmind.audit.AuditOutcome;
import com.np3tech.hoopmind.audit.AuditTarget;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Recherche dans le journal d'audit.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditQueryService {

    private final AuditEntryRepository entries;

    public Page<AuditEntry> search(AuditSearch search, Pageable pageable) {
        return entries.findAll(toSpecification(search), pageable);
    }

    private static Specification<AuditEntry> toSpecification(AuditSearch search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search.action() != null) {
                predicates.add(cb.equal(root.get("action"), search.action()));
            }
            if (search.outcome() != null) {
                predicates.add(cb.equal(root.get("outcome"), search.outcome()));
            }
            if (search.userId() != null) {
                predicates.add(cb.or(
                        cb.equal(root.get("actorId"), search.userId()),
                        cb.and(cb.equal(root.get("targetType"), AuditTarget.USER),
                                cb.equal(root.get("targetId"), search.userId().toString()))));
            }
            if (StringUtils.hasText(search.user())) {
                String pattern = "%" + search.user().trim().toLowerCase()
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("actorLogin")), pattern, '\\'),
                        cb.like(cb.lower(root.get("targetLabel")), pattern, '\\')));
            }
            if (search.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), search.from()));
            }
            if (search.to() != null) {
                predicates.add(cb.lessThan(root.get("occurredAt"), search.to()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * Critères de recherche ; un critère {@code null} n'est pas appliqué.
     *
     * @param userId utilisateur auteur ou cible de l'action
     * @param user   texte recherché dans le nom de l'auteur ou de la cible
     * @param from   borne incluse
     * @param to     borne exclue
     */
    public record AuditSearch(AuditAction action, AuditOutcome outcome, Long userId, String user,
                              Instant from, Instant to) {
    }
}
