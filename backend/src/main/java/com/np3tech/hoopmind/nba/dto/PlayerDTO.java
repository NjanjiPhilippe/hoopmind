package com.np3tech.hoopmind.nba.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private Integer jerseyNumber;
    private String position;
    private LocalDate dateOfBirth;
    private Double height;
    private Double weight;
    private Long teamId;
    private String photoUrl;
    private boolean active;
}