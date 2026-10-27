package com.np3tech.hoopmind.nba.mapper;

import com.np3tech.hoopmind.nba.dto.response.PlayerResponse;
import com.np3tech.hoopmind.nba.dto.response.TeamSummaryResponse;
import com.np3tech.hoopmind.nba.entity.Player;
import org.springframework.stereotype.Component;

@Component
public class PlayerMapper {

    public PlayerResponse toResponse(Player player) {
        return PlayerResponse.builder()
                .id(player.getId())
                .firstName(player.getFirstName())
                .lastName(player.getLastName())
                .position(player.getPosition())
                .jerseyNumber(player.getJerseyNumber())
                .team(TeamSummaryResponse.of(player.getTeam()))
                .birthDate(player.getBirthDate())
                .birthPlace(player.getBirthPlace())
                .nationality(player.getNationality())
                .height(player.getHeight())
                .weight(player.getWeight())
                .experience(player.getExperience())
                .college(player.getCollege())
                .draftYear(player.getDraftYear())
                .draftRound(player.getDraftRound())
                .draftNumber(player.getDraftNumber())
                .photoUrl(player.getPhotoUrl())
                .status(player.getStatus())
                .build();
    }
}
