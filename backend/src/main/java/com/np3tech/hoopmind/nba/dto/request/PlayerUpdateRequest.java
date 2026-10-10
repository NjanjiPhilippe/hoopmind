package com.np3tech.hoopmind.nba.dto.request;

import com.np3tech.hoopmind.nba.entity.enummeration.Position;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerUpdateRequest {
    private String firstName;
    private String lastName;
    private Position position;
    private Integer jerseyNumber;
    private Long teamId;
    private Double height;
    private Double weight;
    private String photoUrl;
}
