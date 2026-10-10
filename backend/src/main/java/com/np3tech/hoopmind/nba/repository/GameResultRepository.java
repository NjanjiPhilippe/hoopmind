package com.np3tech.hoopmind.nba.repository;

import com.np3tech.hoopmind.nba.entity.Game;
import com.np3tech.hoopmind.nba.entity.GameResult;
import com.np3tech.hoopmind.nba.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameResultRepository extends JpaRepository<GameResult, Long> {

    Optional<GameResult> findByGame(Game game);

    List<GameResult> findByWinner(Team winner);

    List<GameResult> findByLoser(Team loser);

    List<GameResult> findByOvertimeTrue();

    @Query("SELECT gr FROM GameResult gr WHERE gr.winner = :team OR gr.loser = :team")
    List<GameResult> findByTeam(@Param("team") Team team);

    @Query("SELECT gr FROM GameResult gr WHERE gr.scoreDifference >= :minDiff ORDER BY gr.scoreDifference DESC")
    List<GameResult> findBlowoutGames(@Param("minDiff") Integer minDifference);
}
