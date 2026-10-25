package com.np3tech.hoopmind.nba.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Score final d'un match, saisi par un administrateur quand l'import n'est pas disponible. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameResultRequest {

    @NotNull(message = "Le score de l'équipe à domicile est obligatoire")
    @Min(value = 0, message = "Un score ne peut pas être négatif")
    @Max(value = 250, message = "Un score dépasse rarement 200 points : vérifiez la saisie")
    private Integer homeTeamScore;

    @NotNull(message = "Le score de l'équipe à l'extérieur est obligatoire")
    @Min(value = 0, message = "Un score ne peut pas être négatif")
    @Max(value = 250, message = "Un score dépasse rarement 200 points : vérifiez la saisie")
    private Integer awayTeamScore;

    @Schema(description = "Nombre de prolongations (0 par défaut)")
    @Min(value = 0, message = "Le nombre de prolongations ne peut pas être négatif")
    @Max(value = 10, message = "Pas plus de 10 prolongations")
    private Integer overtimePeriods;
}
