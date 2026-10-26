package com.np3tech.hoopmind.nba.dto.request;

import com.np3tech.hoopmind.nba.entity.enummeration.PlayerStatus;
import com.np3tech.hoopmind.nba.entity.enummeration.Position;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Les champs absents ne changent pas. Taille en centimètres, poids en kilogrammes. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerUpdateRequest {
    @Size(min = 1, max = 100)
    private String firstName;
    @Size(min = 1, max = 100)
    private String lastName;
    private Position position;
    @Min(0) @Max(99)
    private Integer jerseyNumber;
    private Long teamId;
    @Positive @Max(260)
    private Double height;
    @Positive @Max(200)
    private Double weight;
    @Size(max = 500)
    private String photoUrl;
    private PlayerStatus status;
}
