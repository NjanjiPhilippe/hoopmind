package com.np3tech.hoopmind.nba.criteria;

import com.np3tech.hoopmind.nba.entity.enummeration.PlayerStatus;
import com.np3tech.hoopmind.nba.entity.enummeration.Position;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerCriteria {
    private String firstName;
    private String lastName;
    private Position position;
    private Integer jerseyNumber;
    private String teamName;
    private String birthDateStart;
    private String birthDateEnd;
    private String nationality;
    private Double heightStart;
    private Double heightEnd;
    private Double weightStart;
    private Double weightEnd;
    private Integer draftYear;
    private PlayerStatus status;
    private boolean order;
    private String typeClassement;

}
