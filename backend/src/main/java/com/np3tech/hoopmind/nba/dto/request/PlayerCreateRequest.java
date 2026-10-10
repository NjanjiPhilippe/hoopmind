package com.np3tech.hoopmind.nba.dto.request;

import com.np3tech.hoopmind.nba.entity.enummeration.Position;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerCreateRequest {
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotNull(message = "Position is required")
    private Position position;

    private Integer jerseyNumber;

    @NotNull(message = "Team ID is required")
    private Long teamId;

    private LocalDate birthDate;
    private String birthPlace;
    private String nationality;
    private Double height;
    private Double weight;
    private Integer experience;
    private String college;
    private Integer draftYear;
    private Integer draftRound;
    private Integer draftNumber;
    private String photoUrl;
}
