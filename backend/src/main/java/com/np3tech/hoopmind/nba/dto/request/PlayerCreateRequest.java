package com.np3tech.hoopmind.nba.dto.request;

import com.np3tech.hoopmind.nba.entity.enummeration.Position;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Taille en centimètres, poids en kilogrammes. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerCreateRequest {
    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @NotNull(message = "Position is required")
    private Position position;

    @Min(0) @Max(99)
    private Integer jerseyNumber;

    @NotNull(message = "Team ID is required")
    private Long teamId;

    private LocalDate birthDate;
    private String birthPlace;
    private String nationality;
    @Positive @Max(260)
    private Double height;
    @Positive @Max(200)
    private Double weight;
    private Integer experience;
    private String college;
    private Integer draftYear;
    private Integer draftRound;
    private Integer draftNumber;
    private String photoUrl;
}
