package com.np3tech.hoopmind.nba.dto.response;

import com.np3tech.hoopmind.nba.entity.enummeration.PlayerStatus;
import com.np3tech.hoopmind.nba.entity.enummeration.Position;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private Position position;
    private Integer jerseyNumber;
    private TeamSummaryResponse team;
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
    private PlayerStatus status;
}
