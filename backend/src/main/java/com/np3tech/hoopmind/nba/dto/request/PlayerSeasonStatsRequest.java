package com.np3tech.hoopmind.nba.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Statistiques d'un joueur sur une saison, saisies par un administrateur : moyennes par match, pourcentages entre 0
 * et 1 (0,475 pour 47,5 %). Seuls les matchs joués sont obligatoires.
 */
public record PlayerSeasonStatsRequest(
        @NotNull @PositiveOrZero @Max(110) Integer gamesPlayed,
        @PositiveOrZero @Max(110) Integer gamesStarted,
        @DecimalMin("0") @DecimalMax("60") BigDecimal minutesPerGame,
        @DecimalMin("0") @DecimalMax("100") BigDecimal pointsPerGame,
        @DecimalMin("0") @DecimalMax("40") BigDecimal totalReboundsPerGame,
        @DecimalMin("0") @DecimalMax("30") BigDecimal assistsPerGame,
        @DecimalMin("0") @DecimalMax("15") BigDecimal stealsPerGame,
        @DecimalMin("0") @DecimalMax("15") BigDecimal blocksPerGame,
        @DecimalMin("0") @DecimalMax("15") BigDecimal turnoversPerGame,
        @DecimalMin("0") @DecimalMax("1") BigDecimal fieldGoalPercentage,
        @DecimalMin("0") @DecimalMax("1") BigDecimal threePointPercentage,
        @DecimalMin("0") @DecimalMax("1") BigDecimal freeThrowPercentage) {
}
