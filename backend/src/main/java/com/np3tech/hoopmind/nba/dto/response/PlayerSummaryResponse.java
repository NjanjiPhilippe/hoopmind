package com.np3tech.hoopmind.nba.dto.response;

import com.np3tech.hoopmind.nba.entity.enummeration.Position;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerSummaryResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private Position position;
    private Integer jerseyNumber;
    private String photoUrl;
}
