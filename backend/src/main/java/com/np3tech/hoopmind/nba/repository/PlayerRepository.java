package com.np3tech.hoopmind.nba.repository;

import com.np3tech.hoopmind.nba.entity.Player;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

import com.np3tech.hoopmind.nba.entity.Player;
import com.np3tech.hoopmind.nba.entity.Team;
import com.np3tech.hoopmind.nba.entity.enummeration.PlayerStatus;
import com.np3tech.hoopmind.nba.entity.enummeration.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long>, JpaSpecificationExecutor<Player> {

    List<Player> findByTeam(Team team);

    List<Player> findByPosition(Position position);

    List<Player> findByStatus(PlayerStatus status);

    List<Player> findByTeamAndStatus(Team team, PlayerStatus status);

    @Query("SELECT p FROM Player p WHERE LOWER(p.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Player> searchByName(@Param("name") String name);

    Optional<Player> findByFirstNameAndLastNameAndTeam(String firstName, String lastName, Team team);

    List<Player> findByTeamAndJerseyNumber(Team team, Integer jerseyNumber);
}
