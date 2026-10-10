package com.np3tech.hoopmind.nba.specification;

import com.np3tech.hoopmind.nba.criteria.PlayerCriteria;
import com.np3tech.hoopmind.nba.entity.Player;
import com.np3tech.hoopmind.nba.entity.Team;
import com.np3tech.hoopmind.nba.entity.enummeration.PlayerStatus;
import com.np3tech.hoopmind.nba.entity.enummeration.Position;
import com.np3tech.hoopmind.shared.utils.MethodUtils;
import jakarta.persistence.criteria.Join;
import org.springframework.data.annotation.Transient;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
public class PlayerSpecification {
    private PlayerSpecification() {}

    @Transient
    static Specification<Player> withDateDebut(final String dateDebut) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            LocalDate datetimeBefore = MethodUtils.convertorDate(dateDebut);
            return criteriaBuilder.greaterThanOrEqualTo(root.get("birthDate"), datetimeBefore);
        };
    }
    @Transient
    static Specification<Player> withDateFin(final String dateFin) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            LocalDate datetimeBefore = MethodUtils.convertorDate(dateFin);
            return criteriaBuilder.greaterThanOrEqualTo(root.get("birthDate"), datetimeBefore);
        };
    }

    @Transient
    static Specification<Player> withFirstName(final String clientName) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("firstName"), "%".concat(clientName).concat("%"));
    }
    @Transient
    static Specification<Player> withLastName(final String lastName) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("lastName"), "%".concat(lastName).concat("%"));
    }

    @Transient
    private static Specification<Player> withStatut(final PlayerStatus responseStatus) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), responseStatus);
    }
    @Transient
    private static Specification<Player> withPosition(final Position position) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("position"), position);
    }

    @Transient
    private static Specification<Player> withByTeamName(final String name) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            Join<Player, Team> join = root.join("client");
            return criteriaBuilder.like(join.get("name"), "%".concat(name).concat("%"));
        };
    }
    @Transient
    private static Specification<Player> withByNationality(final String nationality) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("nationality"), "%".concat(nationality).concat("%"));
    }
    @Transient
    private static Specification<Player> withByHeightStart(final Double nationality) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("height"), nationality);
    }
    @Transient
    private static Specification<Player> withByHeightEnd(final Double nationality) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get("height"), nationality);
    }

    @Transient
    private static Specification<Player> withByWeightStart(final Double nationality) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("weight"), nationality);
    }
    @Transient
    private static Specification<Player> withByWeightEnd(final Double nationality) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get("weight"), nationality);
    }
    @Transient
    private static Specification<Player> orderByIdDesc(String column) {
        return (root, criteriaQuery, criteriaBuilder) -> criteriaQuery.orderBy(criteriaBuilder.desc(root.get(column))).getRestriction();
    }

    @Transient
    private static Specification<Player> orderByIdAsc(String column) {
        return (root, criteriaQuery, criteriaBuilder) -> criteriaQuery.orderBy(criteriaBuilder.asc(root.get(column))).getRestriction();
    }

    public static Specification<Player> getSpecification(final PlayerCriteria criteria) {

        Specification<Player> specification = null;

        if (criteria.isOrder()) {
            specification = Specification.allOf(
                    specification,
                    orderByIdAsc(criteria.getTypeClassement())
            );
        }
        if (!criteria.isOrder()) {
            specification = Specification.allOf(
                    specification,
                    orderByIdDesc(criteria.getTypeClassement())
            );        }
        if (!MethodUtils.isNullOrEmpty(criteria.getTeamName())) {
            specification = Specification.allOf(
                    specification, withByTeamName(criteria.getTeamName())
            );
        }
        if (!MethodUtils.isNullOrEmpty(criteria.getNationality())) {
            specification = Specification.allOf(
                    specification, withByNationality(criteria.getNationality())
            );
        }
        if (!MethodUtils.isNullOrEmpty(criteria.getLastName())) {
            specification = Specification.allOf(
                    specification, withLastName(criteria.getLastName())
            );
        }
        if (!MethodUtils.isNullOrEmpty(criteria.getFirstName())) {
            specification = Specification.allOf(
                    specification, withFirstName(criteria.getFirstName())
            );
        }
        if (!MethodUtils.isNullOrEmpty(criteria.getFirstName())) {
            specification = Specification.allOf(
                    specification, withFirstName(criteria.getFirstName())
            );
        }
        if (!MethodUtils.isNullOrEmpty(criteria.getFirstName())) {
            specification = Specification.allOf(
                    specification, withFirstName(criteria.getFirstName())
            );
        }
        if (!MethodUtils.isNullOrEmpty(criteria.getLastName())) {
            specification = Specification.allOf(
                    specification, withFirstName(criteria.getLastName())
            );
        }
        if (!MethodUtils.isNullOrEmpty(criteria.getBirthDateStart())) {
            specification = Specification.allOf(
                    specification, withDateDebut(criteria.getBirthDateStart())
            );
        }
        if (!MethodUtils.isNullOrEmpty(criteria.getBirthDateEnd())) {
            specification = Specification.allOf(
                    specification, withDateFin(criteria.getBirthDateEnd())
            );
        }
        if (criteria.getHeightStart() != null ) {
            specification = Specification.allOf(
                    specification, withByHeightStart(criteria.getHeightStart())
            );
        }
        if (criteria.getHeightEnd() != null ) {
            specification = Specification.allOf(
                    specification, withByHeightEnd(criteria.getHeightEnd())
            );
        }
        if (criteria.getWeightStart() != null ) {
            specification = Specification.allOf(
                    specification, withByWeightStart(criteria.getWeightStart())
            );
        }
        if (criteria.getWeightEnd() != null ) {
            specification = Specification.allOf(
                    specification, withByWeightEnd(criteria.getWeightEnd())
            );
        }
        if (criteria.getPosition() != null ) {
            specification = Specification.allOf(
                    specification, withPosition(criteria.getPosition())
            );
        }
        if (criteria.getStatus() != null ) {
            specification = Specification.allOf(
                    specification, withStatut(criteria.getStatus())
            );
        }
        return specification;
    }
}
