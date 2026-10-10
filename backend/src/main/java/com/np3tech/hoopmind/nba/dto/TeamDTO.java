package com.np3tech.hoopmind.nba.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamDTO {
    private Long id;
    private String name;
    private String city;
    private String abbreviation;
    private String conference;
    private String division;
    private String logoUrl;
}
