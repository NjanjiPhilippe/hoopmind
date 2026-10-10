package com.np3tech.hoopmind.nba.dto.request;

import com.np3tech.hoopmind.nba.entity.enummeration.Conference;
import com.np3tech.hoopmind.nba.entity.enummeration.Division;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamCreateRequest {
    @NotBlank(message = "Team name is required")
    private String name;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Abbreviation is required")
    private String abbreviation;

    @NotNull(message = "Conference is required")
    private Conference conference;

    @NotNull(message = "Division is required")
    private Division division;

    private String logoUrl;
    private Integer foundedYear;
    private String arena;
    private String headCoach;
}
